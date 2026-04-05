package com.satyam.quotation.service.impl;

import com.satyam.quotation.dto.InvoiceDTO;
import com.satyam.quotation.dto.InvoiceItemDTO;
import com.satyam.quotation.dto.InvoiceRequestDTO;
import com.satyam.quotation.dto.PaymentDTO;
import com.satyam.quotation.exception.ResourceNotFoundException;
import com.satyam.quotation.model.*;
import com.satyam.quotation.repository.*;
import com.satyam.quotation.service.InvoiceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceServiceImpl.class);

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final InvoicePaymentRepository invoicePaymentRepository;
    private final InvoiceSequenceRepository invoiceSequenceRepository;
    private final QuotationRepository quotationRepository;
    private final CustomerRepository customerRepository;
    private final CompanyRepository companyRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public InvoiceServiceImpl(InvoiceRepository invoiceRepository,
                             InvoiceItemRepository invoiceItemRepository,
                             InvoicePaymentRepository invoicePaymentRepository,
                             InvoiceSequenceRepository invoiceSequenceRepository,
                             QuotationRepository quotationRepository,
                             CustomerRepository customerRepository,
                             CompanyRepository companyRepository,
                             ProductRepository productRepository,
                             UserRepository userRepository) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.invoicePaymentRepository = invoicePaymentRepository;
        this.invoiceSequenceRepository = invoiceSequenceRepository;
        this.quotationRepository = quotationRepository;
        this.customerRepository = customerRepository;
        this.companyRepository = companyRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public InvoiceDTO createInvoice(InvoiceRequestDTO requestDTO, Long userId) {
        log.info("Creating invoice from quotation: {}", requestDTO.getQuotationId());

        // Get quotation
        Quotation quotation = quotationRepository.findById(requestDTO.getQuotationId())
                .orElseThrow(() -> new ResourceNotFoundException("Quotation not found"));

        // Validate quotation is approved
        if (!"APPROVED".equals(quotation.getStatus())) {
            throw new IllegalStateException("Only approved quotations can be converted to invoices");
        }

        // Create invoice
        Invoice invoice = new Invoice();
        invoice.setQuotation(quotation);
        invoice.setCustomer(quotation.getCustomer());
        invoice.setCompany(quotation.getCompany());
        invoice.setInvoiceDate(requestDTO.getInvoiceDate() != null ? requestDTO.getInvoiceDate() : LocalDate.now());
        invoice.setDueDate(requestDTO.getDueDate() != null ? requestDTO.getDueDate() : LocalDate.now().plusDays(30));
        invoice.setDiscountPercentage(requestDTO.getDiscountPercentage() != null ? requestDTO.getDiscountPercentage() : BigDecimal.ZERO);
        invoice.setNotes(requestDTO.getNotes());
        invoice.setTermsAndConditions(requestDTO.getTermsAndConditions());
        invoice.setStatus("DRAFT");
        invoice.setPaymentStatus("PENDING");
        invoice.setCreatedBy(userId);
        invoice.setCreatedAt(LocalDateTime.now());
        invoice.setActive(true);

        // Generate invoice number
        String invoiceNumber = generateInvoiceNumber(quotation.getCompany().getId());
        invoice.setInvoiceNumber(invoiceNumber);

        // Save invoice first
        Invoice savedInvoice = invoiceRepository.save(invoice);

        // Copy items from quotation
        if (quotation.getItems() != null && !quotation.getItems().isEmpty()) {
            for (QuotationItem qItem : quotation.getItems()) {
                InvoiceItem invoiceItem = new InvoiceItem();
                invoiceItem.setInvoice(savedInvoice);
                invoiceItem.setProduct(qItem.getProduct());
                invoiceItem.setProductName(qItem.getProductName() != null ? qItem.getProductName() : qItem.getProductNameSnapshot());
                invoiceItem.setProductDescription(qItem.getProductDescription() != null ? qItem.getProductDescription() : qItem.getProductDescriptionSnapshot());
                invoiceItem.setQuantity(qItem.getQuantity());
                invoiceItem.setUnitPrice(qItem.getUnitPrice());
                invoiceItem.setDiscountPercentage(qItem.getDiscountPercentage());
                invoiceItem.setTaxPercentage(qItem.getTaxPercentage());
                invoiceItem.setTaxAmount(qItem.getTaxAmount());
                invoiceItem.setItemTotal(qItem.getItemTotal());
                invoiceItem.setTotal(qItem.getTotal());
                invoiceItemRepository.save(invoiceItem);
            }
        }

        // Calculate totals
        calculateInvoiceTotals(savedInvoice);

        log.info("Created invoice: {} from quotation: {}", invoiceNumber, quotation.getQuotationNumber());

        return convertToDTO(savedInvoice);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InvoiceDTO> getInvoiceById(Long id) {
        return invoiceRepository.findById(id)
                .filter(Invoice::getActive)
                .map(this::convertToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceDTO getInvoiceByNumber(String invoiceNumber) {
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .filter(Invoice::getActive)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceNumber));
        return convertToDTO(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceDTO> getInvoicesByCompany(Long companyId) {
        return invoiceRepository.findActiveInvoicesByCompany(companyId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceDTO> getInvoicesByCreatedBy(Long userId) {
        return invoiceRepository.findAll().stream()
                .filter(i -> Boolean.TRUE.equals(i.getActive()) && userId.equals(i.getCreatedBy()))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceDTO> getInvoicesByCustomer(Long customerId) {
        return invoiceRepository.findByCustomerId(customerId).stream()
                .filter(Invoice::getActive)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceDTO> getInvoicesByQuotation(Long quotationId) {
        return invoiceRepository.findByQuotationId(quotationId).stream()
                .filter(Invoice::getActive)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceDTO> getInvoicesByStatus(Long companyId, String status) {
        return invoiceRepository.findActiveInvoicesByCompanyAndStatus(companyId, status).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceDTO> getInvoicesByPaymentStatus(Long companyId, String paymentStatus) {
        return invoiceRepository.findActiveInvoicesByCompanyAndPaymentStatus(companyId, paymentStatus).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public InvoiceDTO updateInvoice(Long id, InvoiceRequestDTO requestDTO, Long userId) {
        Invoice invoice = invoiceRepository.findById(id)
                .filter(Invoice::getActive)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        // Can only edit DRAFT invoices
        if (!"DRAFT".equals(invoice.getStatus())) {
            throw new IllegalStateException("Only DRAFT invoices can be edited");
        }

        // Update fields
        if (requestDTO.getInvoiceDate() != null) {
            invoice.setInvoiceDate(requestDTO.getInvoiceDate());
        }
        if (requestDTO.getDueDate() != null) {
            invoice.setDueDate(requestDTO.getDueDate());
        }
        if (requestDTO.getDiscountPercentage() != null) {
            invoice.setDiscountPercentage(requestDTO.getDiscountPercentage());
        }
        if (requestDTO.getNotes() != null) {
            invoice.setNotes(requestDTO.getNotes());
        }
        if (requestDTO.getTermsAndConditions() != null) {
            invoice.setTermsAndConditions(requestDTO.getTermsAndConditions());
        }

        // Update items if provided
        if (requestDTO.getItems() != null) {
            invoiceItemRepository.deleteAll(invoiceItemRepository.findByInvoiceId(id));
            for (InvoiceItemDTO itemDTO : requestDTO.getItems()) {
                InvoiceItem item = new InvoiceItem();
                item.setInvoice(invoice);
                item.setProduct(productRepository.findById(itemDTO.getProductId())
                        .orElseThrow(() -> new ResourceNotFoundException("Product not found")));
                item.setProductName(itemDTO.getProductName());
                item.setProductDescription(itemDTO.getProductDescription());
                item.setQuantity(itemDTO.getQuantity());
                item.setUnitPrice(itemDTO.getUnitPrice());
                item.setDiscountPercentage(itemDTO.getDiscountPercentage());
                item.setTaxPercentage(itemDTO.getTaxPercentage());
                item.calculateItemTotal();
                invoiceItemRepository.save(item);
            }
        }

        invoice.setUpdatedAt(LocalDateTime.now());
        invoice.setUpdatedBy(userId);

        calculateInvoiceTotals(invoice);
        Invoice saved = invoiceRepository.save(invoice);

        log.info("Updated invoice: {}", saved.getInvoiceNumber());
        return convertToDTO(saved);
    }

    @Override
    @Transactional
    public void deleteInvoice(Long id, Long userId) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        invoice.setActive(false);
        invoice.setDeletedAt(LocalDateTime.now());
        invoice.setDeletedBy(userId);

        invoiceRepository.save(invoice);
        log.info("Deleted invoice: {}", invoice.getInvoiceNumber());
    }

    @Override
    @Transactional
    public InvoiceDTO changeStatus(Long invoiceId, String newStatus, Long userId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .filter(Invoice::getActive)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        // Validate status transition
        validateStatusTransition(invoice.getStatus(), newStatus);

        invoice.setStatus(newStatus);
        invoice.setUpdatedAt(LocalDateTime.now());
        invoice.setUpdatedBy(userId);

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Changed invoice {} status to {}", saved.getInvoiceNumber(), newStatus);

        return convertToDTO(saved);
    }

    @Override
    @Transactional
    public InvoiceDTO markAsPaid(Long invoiceId, Long userId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .filter(Invoice::getActive)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        invoice.setPaymentStatus("PAID");
        invoice.setStatus("PAID");
        invoice.setUpdatedAt(LocalDateTime.now());
        invoice.setUpdatedBy(userId);

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Marked invoice {} as paid", saved.getInvoiceNumber());

        return convertToDTO(saved);
    }

    @Override
    @Transactional
    public InvoiceDTO markAsSent(Long invoiceId, Long userId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .filter(Invoice::getActive)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        invoice.setStatus("SENT");
        invoice.setEmailSent(true);
        invoice.setEmailSentAt(LocalDateTime.now());
        invoice.setUpdatedAt(LocalDateTime.now());
        invoice.setUpdatedBy(userId);

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Marked invoice {} as sent", saved.getInvoiceNumber());

        return convertToDTO(saved);
    }

    @Override
    @Transactional
    public PaymentDTO recordPayment(Long invoiceId, PaymentDTO paymentDTO, Long userId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .filter(Invoice::getActive)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        InvoicePayment payment = new InvoicePayment();
        payment.setInvoice(invoice);
        payment.setPaymentDate(paymentDTO.getPaymentDate());
        payment.setPaymentAmount(paymentDTO.getPaymentAmount());
        payment.setPaymentMethod(paymentDTO.getPaymentMethod());
        payment.setPaymentReference(paymentDTO.getPaymentReference());
        payment.setNotes(paymentDTO.getNotes());
        payment.setCreatedBy(userId);
        payment.setCreatedAt(LocalDateTime.now());

        InvoicePayment saved = invoicePaymentRepository.save(payment);

        // Update invoice payment status
        updateInvoicePaymentStatus(invoice);

        log.info("Recorded payment of {} for invoice {}", paymentDTO.getPaymentAmount(), invoice.getInvoiceNumber());

        return convertPaymentToDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentDTO> getPaymentHistory(Long invoiceId) {
        return invoicePaymentRepository.findByInvoiceId(invoiceId).stream()
                .map(this::convertPaymentToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deletePayment(Long paymentId, Long userId) {
        InvoicePayment payment = invoicePaymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        Invoice invoice = payment.getInvoice();
        invoicePaymentRepository.delete(payment);

        // Update invoice payment status
        updateInvoicePaymentStatus(invoice);

        log.info("Deleted payment: {}", paymentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceDTO> getOverdueInvoices(Long companyId) {
        return invoiceRepository.findOverdueInvoices(companyId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceDTO> getInvoicesByDateRange(Long companyId, LocalDate startDate, LocalDate endDate) {
        return invoiceRepository.findInvoicesByDateRange(companyId, startDate, endDate).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public String generateInvoiceNumber(Long companyId) {
        int currentYear = Year.now().getValue();
        
        InvoiceSequence sequence = invoiceSequenceRepository.findByCompanyIdAndYear(companyId, currentYear)
                .orElse(new InvoiceSequence());
        
        sequence.setCompany(companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found")));
        sequence.setYear(currentYear);
        sequence.setLastSequence(sequence.getLastSequence() + 1);
        
        InvoiceSequence saved = invoiceSequenceRepository.save(sequence);
        
        return String.format("INV-%03d-%04d", companyId, saved.getLastSequence());
    }

    // Helper methods

    private void calculateInvoiceTotals(Invoice invoice) {
        List<InvoiceItem> items = invoiceItemRepository.findByInvoiceId(invoice.getId());
        
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        
        for (InvoiceItem item : items) {
            item.calculateItemTotal();
            subtotal = subtotal.add(item.getItemTotal().subtract(item.getTaxAmount() != null ? item.getTaxAmount() : BigDecimal.ZERO));
            totalTax = totalTax.add(item.getTaxAmount() != null ? item.getTaxAmount() : BigDecimal.ZERO);
        }
        
        invoice.setSubtotal(subtotal);
        invoice.setTotalTax(totalTax);
        
        // Apply discount
        BigDecimal discountAmount = subtotal.multiply(invoice.getDiscountPercentage()).divide(BigDecimal.valueOf(100));
        invoice.setTotalDiscount(discountAmount);
        
        // Calculate total
        BigDecimal total = subtotal.subtract(discountAmount).add(totalTax);
        invoice.setTotalAmount(total);
    }

    private void updateInvoicePaymentStatus(Invoice invoice) {
        BigDecimal totalPaid = invoicePaymentRepository.getTotalPaymentsByInvoiceId(invoice.getId());
        BigDecimal totalAmount = invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO;
        
        if (totalPaid.compareTo(totalAmount) >= 0 && totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            invoice.setPaymentStatus("PAID");
            invoice.setStatus("PAID");
        } else if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
            invoice.setPaymentStatus("PARTIAL");
            // Only update status if not already in a terminal state
            if (!"PAID".equals(invoice.getStatus()) && !"CANCELLED".equals(invoice.getStatus())) {
                invoice.setStatus("PARTIAL");
            }
        } else {
            invoice.setPaymentStatus("PENDING");
        }
        
        invoice.setUpdatedAt(LocalDateTime.now());
        invoiceRepository.save(invoice);
    }

    private void validateStatusTransition(String currentStatus, String newStatus) {
        // Define valid transitions
        switch (currentStatus) {
            case "DRAFT":
                if (!newStatus.equals("SENT") && !newStatus.equals("CANCELLED")) {
                    throw new IllegalStateException("Cannot transition from DRAFT to " + newStatus);
                }
                break;
            case "SENT":
                if (!newStatus.equals("PARTIAL") && !newStatus.equals("PAID") && !newStatus.equals("OVERDUE") && !newStatus.equals("CANCELLED")) {
                    throw new IllegalStateException("Cannot transition from SENT to " + newStatus);
                }
                break;
            case "PARTIAL":
                if (!newStatus.equals("PAID") && !newStatus.equals("OVERDUE") && !newStatus.equals("CANCELLED")) {
                    throw new IllegalStateException("Cannot transition from PARTIAL to " + newStatus);
                }
                break;
            case "PAID":
            case "CANCELLED":
            case "OVERDUE":
                throw new IllegalStateException("Cannot transition from " + currentStatus + " status");
        }
    }

    private InvoiceDTO convertToDTO(Invoice invoice) {
        InvoiceDTO dto = new InvoiceDTO();
        dto.setId(invoice.getId());
        dto.setInvoiceNumber(invoice.getInvoiceNumber());
        dto.setQuotationId(invoice.getQuotation().getId());
        dto.setCustomerId(invoice.getCustomer().getId());
        dto.setCustomerName(invoice.getCustomer().getCustomerName());
        dto.setCompanyId(invoice.getCompany().getId());
        dto.setCompanyName(invoice.getCompany().getCompanyName());
        dto.setInvoiceDate(invoice.getInvoiceDate());
        dto.setDueDate(invoice.getDueDate());
        dto.setSubtotal(invoice.getSubtotal());
        dto.setDiscountPercentage(invoice.getDiscountPercentage());
        dto.setTotalDiscount(invoice.getTotalDiscount());
        dto.setTotalTax(invoice.getTotalTax());
        dto.setTotalAmount(invoice.getTotalAmount());
        dto.setStatus(invoice.getStatus());
        dto.setPaymentStatus(invoice.getPaymentStatus());
        dto.setNotes(invoice.getNotes());
        dto.setTermsAndConditions(invoice.getTermsAndConditions());
        dto.setEmailSent(invoice.getEmailSent());
        dto.setEmailSentAt(invoice.getEmailSentAt());
        dto.setCreatedBy(invoice.getCreatedBy());
        dto.setCreatedAt(invoice.getCreatedAt());
        dto.setUpdatedBy(invoice.getUpdatedBy());
        dto.setUpdatedAt(invoice.getUpdatedAt());
        dto.setActive(invoice.getActive());
        
        // Get items
        List<InvoiceItem> items = invoiceItemRepository.findByInvoiceId(invoice.getId());
        dto.setItems(items.stream().map(this::convertItemToDTO).collect(Collectors.toList()));
        
        // Get payments
        List<InvoicePayment> payments = invoicePaymentRepository.findByInvoiceId(invoice.getId());
        dto.setPayments(payments.stream().map(this::convertPaymentToDTO).collect(Collectors.toList()));
        
        // Calculate totals
        BigDecimal totalPaid = invoicePaymentRepository.getTotalPaymentsByInvoiceId(invoice.getId());
        dto.setTotalPaid(totalPaid);
        BigDecimal totalAmount = invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO;
        dto.setRemainingBalance(totalAmount.subtract(totalPaid));
        
        return dto;
    }

    private InvoiceItemDTO convertItemToDTO(InvoiceItem item) {
        InvoiceItemDTO dto = new InvoiceItemDTO();
        dto.setId(item.getId());
        dto.setInvoiceId(item.getInvoice().getId());
        dto.setProductId(item.getProduct().getId());
        dto.setProductName(item.getProductName());
        dto.setProductDescription(item.getProductDescription());
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setDiscountPercentage(item.getDiscountPercentage());
        dto.setTaxPercentage(item.getTaxPercentage());
        dto.setTaxAmount(item.getTaxAmount());
        dto.setItemTotal(item.getItemTotal());
        dto.setTotal(item.getTotal());
        return dto;
    }

    private PaymentDTO convertPaymentToDTO(InvoicePayment payment) {
        PaymentDTO dto = new PaymentDTO();
        dto.setId(payment.getId());
        dto.setInvoiceId(payment.getInvoice().getId());
        dto.setPaymentDate(payment.getPaymentDate());
        dto.setPaymentAmount(payment.getPaymentAmount());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setPaymentReference(payment.getPaymentReference());
        dto.setNotes(payment.getNotes());
        dto.setCreatedBy(payment.getCreatedBy());
        dto.setCreatedAt(payment.getCreatedAt());
        return dto;
    }
}
