package com.satyam.quotation.service.impl;

import com.satyam.quotation.exception.ResourceNotFoundException;
import com.satyam.quotation.model.Customer;
import com.satyam.quotation.model.Product;
import com.satyam.quotation.model.Quotation;
import com.satyam.quotation.model.QuotationItem;
import com.satyam.quotation.repository.CustomerRepository;
import com.satyam.quotation.repository.ProductRepository;
import com.satyam.quotation.repository.QuotationRepository;
import com.satyam.quotation.repository.UserRepository;
import com.satyam.quotation.service.QuotationBusinessService;
import com.satyam.quotation.service.QuotationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class QuotationServiceImpl implements QuotationService {

    private static final Logger log = LoggerFactory.getLogger(QuotationServiceImpl.class);

    private final QuotationRepository quotationRepository;
    private final UserRepository userRepository;
    private final QuotationBusinessService businessService;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;

    public QuotationServiceImpl(QuotationRepository quotationRepository,
                               UserRepository userRepository,
                               QuotationBusinessService businessService,
                               ProductRepository productRepository,
                               CustomerRepository customerRepository) {
        this.quotationRepository = quotationRepository;
        this.userRepository = userRepository;
        this.businessService = businessService;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
    }

    @Override
    @Transactional
    public Quotation createQuotation(Quotation quotation) {
        // Set default status if not provided (should come from DTO)
        if (quotation.getStatus() == null || quotation.getStatus().isEmpty()) {
            quotation.setStatus("DRAFT");
            log.warn("Status not provided, defaulting to DRAFT");
        } else {
            log.info("Creating quotation with status: {}", quotation.getStatus());
        }

        // Set default currency if not provided
        if (quotation.getCurrency() == null || quotation.getCurrency().isEmpty()) {
            quotation.setCurrency("INR");
        }

        // Load customer if only ID is set
        if (quotation.getCustomer() != null && quotation.getCustomer().getId() != null) {
            Customer customer = customerRepository.findById(quotation.getCustomer().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + quotation.getCustomer().getId()));
            quotation.setCustomer(customer);
        }

        // Generate quotation number
        if (quotation.getCompany() != null) {
            String quotationNumber = businessService.generateQuotationNumber(quotation.getCompany().getId());
            quotation.setQuotationNumber(quotationNumber);
            log.info("Generated quotation number: {}", quotationNumber);
        }

        // Load products and capture snapshots for all items
        if (quotation.getItems() != null) {
            for (QuotationItem item : quotation.getItems()) {
                item.setQuotation(quotation);
                
                // Load full product if only ID is set
                if (item.getProduct() != null && item.getProduct().getId() != null) {
                    Product product = productRepository.findById(item.getProduct().getId())
                            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + item.getProduct().getId()));
                    item.setProduct(product);
                }
                
                // Capture product snapshot
                businessService.captureProductSnapshot(item);
            }
        }

        // Calculate totals
        businessService.calculateTotals(quotation);

        quotation.setCreatedAt(LocalDateTime.now());
        quotation.setActive(true);

        Quotation saved = quotationRepository.save(quotation);
        log.info("Created quotation: {} with {} items", saved.getQuotationNumber(),
                saved.getItems() != null ? saved.getItems().size() : 0);

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Quotation> getQuotationById(Long id) {
        return quotationRepository.findById(id)
                .filter(Quotation::getActive);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Quotation> getQuotationsByCompany(Long companyId) {
        return quotationRepository.findByCompanyId(companyId).stream()
                .filter(Quotation::getActive)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Quotation> getQuotationsByUser(Long userId) {
        return quotationRepository.findByCreatedBy(userId).stream()
                .filter(Quotation::getActive)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Quotation> getAllQuotations() {
        return quotationRepository.findAll().stream()
                .filter(Quotation::getActive)
                .toList();
    }

    @Override
    @Transactional
    public Quotation updateQuotation(Long id, Quotation updatedQuotation, Long userId) {
        Quotation quotation = quotationRepository.findById(id)
                .filter(Quotation::getActive)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation not found"));

        // Check if quotation can be edited
        if (!businessService.canEdit(quotation)) {
            throw new IllegalStateException(
                    String.format("Quotation in %s status cannot be edited", quotation.getStatus()));
        }

        // If status is being changed, validate the transition
        if (updatedQuotation.getStatus() != null &&
                !updatedQuotation.getStatus().equals(quotation.getStatus())) {

            businessService.validateQuotation(quotation, updatedQuotation.getStatus());
        }

        // Update fields
        if (updatedQuotation.getStatus() != null) {
            quotation.setStatus(updatedQuotation.getStatus());
        }
        if (updatedQuotation.getCustomer() != null) {
            quotation.setCustomer(updatedQuotation.getCustomer());
        }
        if (updatedQuotation.getExpiryDate() != null) {
            quotation.setExpiryDate(updatedQuotation.getExpiryDate());
        }
        if (updatedQuotation.getNotes() != null) {
            quotation.setNotes(updatedQuotation.getNotes());
        }
        if (updatedQuotation.getTermsAndConditions() != null) {
            quotation.setTermsAndConditions(updatedQuotation.getTermsAndConditions());
        }
        if (updatedQuotation.getDiscountPercentage() != null) {
            quotation.setDiscountPercentage(updatedQuotation.getDiscountPercentage());
        }

        // Update items if provided
        if (updatedQuotation.getItems() != null) {
            quotation.getItems().clear();
            for (QuotationItem item : updatedQuotation.getItems()) {
                item.setQuotation(quotation);
                businessService.captureProductSnapshot(item);
                quotation.getItems().add(item);
            }
        }

        // Recalculate totals
        businessService.calculateTotals(quotation);

        quotation.setUpdatedAt(LocalDateTime.now());
        quotation.setUpdatedBy(userId);

        Quotation saved = quotationRepository.save(quotation);
        log.info("Updated quotation: {} to status: {}", saved.getQuotationNumber(), saved.getStatus());

        return saved;
    }

    @Override
    @Transactional
    public void deleteQuotation(Long id, Long userId) {
        Quotation quotation = quotationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation not found"));

        quotation.setActive(false);
        quotation.setDeletedAt(LocalDateTime.now());
        quotation.setDeletedBy(userId);

        quotationRepository.save(quotation);
    }

    @Override
    public Double getRevenueByCompany(Long companyId) {
        return quotationRepository.getTotalRevenueByCompany(companyId);
    }

    @Override
    public Double getRevenueByUser(Long userId) {
        return quotationRepository.getTotalRevenueByUser(userId);
    }

    @Override
    @Transactional
    public Quotation duplicateQuotation(Long quotationId, Long userId) {
        log.info("Duplicating quotation {} by user {}", quotationId, userId);
        return businessService.duplicateQuotation(quotationId, userId);
    }

    @Override
    @Transactional
    public Quotation changeStatus(Long quotationId, String newStatus, Long userId) {
        Quotation quotation = quotationRepository.findById(quotationId)
                .filter(Quotation::getActive)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation not found"));

        // Validate status change
        businessService.validateQuotation(quotation, newStatus);

        // Update status
        quotation.setStatus(newStatus);
        quotation.setUpdatedAt(LocalDateTime.now());

        quotation.setUpdatedBy(userId);

        Quotation saved = quotationRepository.save(quotation);
        log.info("Changed quotation {} status to {}", saved.getQuotationNumber(), newStatus);

        return saved;
    }

    @Override
    public List<Quotation> getQuotationsByStatus(String status, Long companyId) {
        return businessService.getQuotationsByStatus(status, companyId);
    }

    @Override
    public List<Quotation> getExpiredQuotations(Long companyId) {
        return businessService.getExpiredQuotations(companyId);
    }
}
