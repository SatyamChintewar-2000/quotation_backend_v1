package com.satyam.quotation.service.impl;

import com.satyam.quotation.exception.ResourceNotFoundException;
import com.satyam.quotation.model.*;
import com.satyam.quotation.repository.*;
import com.satyam.quotation.service.QuotationBusinessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class QuotationBusinessServiceImpl implements QuotationBusinessService {

    private static final Logger log = LoggerFactory.getLogger(QuotationBusinessServiceImpl.class);

    private final QuotationSequenceRepository sequenceRepository;
    private final CompanyRepository companyRepository;
    private final QuotationRepository quotationRepository;
    private final UserRepository userRepository;

    public QuotationBusinessServiceImpl(QuotationSequenceRepository sequenceRepository,
                                       CompanyRepository companyRepository,
                                       QuotationRepository quotationRepository,
                                       UserRepository userRepository) {
        this.sequenceRepository = sequenceRepository;
        this.companyRepository = companyRepository;
        this.quotationRepository = quotationRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public String generateQuotationNumber(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        int currentYear = LocalDate.now().getYear();

        // Get or create sequence with pessimistic lock to prevent duplicates
        QuotationSequence sequence = sequenceRepository
                .findByCompanyAndYearForUpdate(company, currentYear)
                .orElseGet(() -> {
                    QuotationSequence newSeq = QuotationSequence.builder()
                            .company(company)
                            .year(currentYear)
                            .lastSequence(0)
                            .build();
                    return sequenceRepository.save(newSeq);
                });

        // Increment sequence
        sequence.setLastSequence(sequence.getLastSequence() + 1);
        sequenceRepository.save(sequence);

        // Format: QT-{companyId}-{sequence}
        String quotationNumber = String.format("QT-%03d-%04d",
                companyId,
                sequence.getLastSequence());

        log.info("Generated quotation number: {} for company: {}", quotationNumber, companyId);

        return quotationNumber;
    }

    @Override
    public boolean canEdit(Quotation quotation) {
        // Only DRAFT quotations can be edited
        return "DRAFT".equals(quotation.getStatus());
    }

    @Override
    public boolean canChangeStatus(Quotation quotation, String targetStatus) {
        String currentStatus = quotation.getStatus();

        // Define valid status transitions
        // GENERATED can go to SENT (when notifications on) or APPROVED (when notifications off)
        // GENERATED cannot go back to DRAFT
        return switch (currentStatus) {
            case "DRAFT" -> List.of("GENERATED", "REJECTED").contains(targetStatus);
            case "GENERATED" -> List.of("SENT", "APPROVED").contains(targetStatus);
            case "SENT" -> List.of("APPROVED", "REJECTED").contains(targetStatus);
            case "APPROVED", "REJECTED" -> false; // Terminal states
            default -> false;
        };
    }

    @Override
    public boolean isExpired(Quotation quotation) {
        if (quotation.getExpiryDate() == null) {
            return false;
        }
        return LocalDate.now().isAfter(quotation.getExpiryDate());
    }

    @Override
    public void calculateTotals(Quotation quotation) {
        if (quotation.getItems() == null || quotation.getItems().isEmpty()) {
            quotation.setSubtotal(BigDecimal.ZERO);
            quotation.setTotalDiscount(BigDecimal.ZERO);
            quotation.setTotalGst(BigDecimal.ZERO);
            quotation.setTotalAmount(BigDecimal.ZERO);
            return;
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        // Calculate each item
        for (QuotationItem item : quotation.getItems()) {
            // Skip items with null unit price
            if (item.getUnitPrice() == null) {
                log.warn("Skipping item with null unit price");
                continue;
            }
            
            item.calculateItemTotal();

            // Accumulate totals
            BigDecimal baseAmount = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            
            // Handle null discount percentage
            BigDecimal discountPercentage = item.getDiscountPercentage() != null ? item.getDiscountPercentage() : BigDecimal.ZERO;
            BigDecimal itemDiscount = baseAmount.multiply(discountPercentage)
                    .divide(BigDecimal.valueOf(100));

            subtotal = subtotal.add(baseAmount);
            totalDiscount = totalDiscount.add(itemDiscount);
            
            // Handle null tax amount
            BigDecimal taxAmount = item.getTaxAmount() != null ? item.getTaxAmount() : BigDecimal.ZERO;
            totalTax = totalTax.add(taxAmount);
        }

        // Apply quotation-level discount if any
        if (quotation.getDiscountPercentage() != null &&
                quotation.getDiscountPercentage().compareTo(BigDecimal.ZERO) > 0) {

            BigDecimal quotationDiscount = subtotal.multiply(quotation.getDiscountPercentage())
                    .divide(BigDecimal.valueOf(100));
            totalDiscount = totalDiscount.add(quotationDiscount);
        }

        // Set totals
        quotation.setSubtotal(subtotal);
        quotation.setTotalDiscount(totalDiscount);
        quotation.setTotalGst(totalTax);

        // Grand total = subtotal - total discount + total tax
        BigDecimal grandTotal = subtotal.subtract(totalDiscount).add(totalTax);
        quotation.setTotalAmount(grandTotal);

        log.debug("Calculated totals for quotation {}: subtotal={}, discount={}, tax={}, total={}",
                quotation.getQuotationNumber(), subtotal, totalDiscount, totalTax, grandTotal);
    }

    @Override
    public void captureProductSnapshot(QuotationItem item) {
        if (item.getProduct() == null) {
            log.warn("Cannot capture product snapshot: product is null");
            return;
        }

        Product product = item.getProduct();

        // Capture product details at time of quoting
        item.setProductNameSnapshot(product.getProductName());
        item.setProductDescriptionSnapshot(product.getDescription());
        item.setUnitSnapshot(product.getUnit());

        // Set legacy fields for backward compatibility
        item.setProductName(product.getProductName());
        item.setProductDescription(product.getDescription());

        // Set unit price from product if not already set
        if (item.getUnitPrice() == null) {
            item.setUnitPrice(product.getPrice());
        }

        // Set tax percentage from product if not already set
        if (item.getTaxPercentage() == null || item.getTaxPercentage().compareTo(BigDecimal.ZERO) == 0) {
            item.setTaxPercentage(product.getTaxPercentage());
        }

        log.debug("Captured product snapshot for item: product={}, price={}, tax={}",
                product.getProductName(), item.getUnitPrice(), item.getTaxPercentage());
    }

    @Override
    @Transactional
    public Quotation duplicateQuotation(Long quotationId, Long userId) {
        Quotation original = quotationRepository.findById(quotationId)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation not found"));

        // Create new quotation
        Quotation duplicate = Quotation.builder()
                .company(original.getCompany())
                .customer(original.getCustomer())
                .createdBy(userId)
                .status("DRAFT")
                .currency(original.getCurrency())
                .expiryDate(LocalDate.now().plusDays(30)) // New expiry date
                .notes(original.getNotes())
                .termsAndConditions(original.getTermsAndConditions())
                .discountPercentage(original.getDiscountPercentage())
                .createdAt(LocalDateTime.now())
                .active(true)
                .build();

        // Generate new quotation number
        duplicate.setQuotationNumber(generateQuotationNumber(original.getCompany().getId()));

        // Duplicate items
        List<QuotationItem> duplicateItems = new ArrayList<>();
        for (QuotationItem originalItem : original.getItems()) {
            QuotationItem duplicateItem = QuotationItem.builder()
                    .quotation(duplicate)
                    .product(originalItem.getProduct())
                    .productNameSnapshot(originalItem.getProductNameSnapshot())
                    .productDescriptionSnapshot(originalItem.getProductDescriptionSnapshot())
                    .unitSnapshot(originalItem.getUnitSnapshot())
                    .quantity(originalItem.getQuantity())
                    .unitPrice(originalItem.getUnitPrice())
                    .discountPercentage(originalItem.getDiscountPercentage())
                    .taxPercentage(originalItem.getTaxPercentage())
                    .build();

            duplicateItem.calculateItemTotal();
            duplicateItems.add(duplicateItem);
        }

        duplicate.setItems(duplicateItems);

        // Calculate totals
        calculateTotals(duplicate);

        // Save
        Quotation saved = quotationRepository.save(duplicate);

        log.info("Duplicated quotation {} to new quotation {} by user {}",
                original.getQuotationNumber(), saved.getQuotationNumber(), userId);

        return saved;
    }

    @Override
    public void validateQuotation(Quotation quotation, String targetStatus) {
        // Check if quotation has items
        if (quotation.getItems() == null || quotation.getItems().isEmpty()) {
            throw new IllegalStateException("Quotation must have at least one item");
        }

        // Check if expiry date is set
        if (quotation.getExpiryDate() == null) {
            throw new IllegalStateException("Expiry date is required");
        }

        // Check if customer is set
        if (quotation.getCustomer() == null) {
            throw new IllegalStateException("Customer is required");
        }

        // Cannot approve expired quotation
        if ("APPROVED".equals(targetStatus) && isExpired(quotation)) {
            throw new IllegalStateException("Cannot approve expired quotation");
        }

        // Check if status transition is valid
        if (!canChangeStatus(quotation, targetStatus)) {
            throw new IllegalStateException(
                    String.format("Cannot change status from %s to %s",
                            quotation.getStatus(), targetStatus));
        }

        log.debug("Validation passed for quotation {} changing to status {}",
                quotation.getQuotationNumber(), targetStatus);
    }

    @Override
    public List<Quotation> getQuotationsByStatus(String status, Long companyId) {
        return quotationRepository.findAll().stream()
                .filter(Quotation::getActive)
                .filter(q -> status.equals(q.getStatus()))
                .filter(q -> companyId == null || companyId.equals(q.getCompany().getId()))
                .toList();
    }

    @Override
    public List<Quotation> getExpiredQuotations(Long companyId) {
        return quotationRepository.findAll().stream()
                .filter(Quotation::getActive)
                .filter(this::isExpired)
                .filter(q -> companyId == null || companyId.equals(q.getCompany().getId()))
                .toList();
    }
}
