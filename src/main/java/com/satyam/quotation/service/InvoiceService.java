package com.satyam.quotation.service;

import com.satyam.quotation.dto.InvoiceDTO;
import com.satyam.quotation.dto.InvoiceRequestDTO;
import com.satyam.quotation.dto.PaymentDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InvoiceService {
    
    // Invoice CRUD operations
    InvoiceDTO createInvoice(InvoiceRequestDTO requestDTO, Long userId);
    
    Optional<InvoiceDTO> getInvoiceById(Long id);
    
    InvoiceDTO getInvoiceByNumber(String invoiceNumber);
    
    List<InvoiceDTO> getInvoicesByCompany(Long companyId);

    List<InvoiceDTO> getInvoicesByCreatedBy(Long userId);
    
    List<InvoiceDTO> getInvoicesByCustomer(Long customerId);
    
    List<InvoiceDTO> getInvoicesByQuotation(Long quotationId);
    
    List<InvoiceDTO> getInvoicesByStatus(Long companyId, String status);
    
    List<InvoiceDTO> getInvoicesByPaymentStatus(Long companyId, String paymentStatus);
    
    InvoiceDTO updateInvoice(Long id, InvoiceRequestDTO requestDTO, Long userId);
    
    void deleteInvoice(Long id, Long userId);
    
    // Invoice status operations
    InvoiceDTO changeStatus(Long invoiceId, String newStatus, Long userId);
    
    InvoiceDTO markAsPaid(Long invoiceId, Long userId);
    
    InvoiceDTO markAsSent(Long invoiceId, Long userId);
    
    // Payment operations
    PaymentDTO recordPayment(Long invoiceId, PaymentDTO paymentDTO, Long userId);
    
    List<PaymentDTO> getPaymentHistory(Long invoiceId);
    
    void deletePayment(Long paymentId, Long userId);
    
    // Invoice queries
    List<InvoiceDTO> getOverdueInvoices(Long companyId);
    
    List<InvoiceDTO> getInvoicesByDateRange(Long companyId, LocalDate startDate, LocalDate endDate);
    
    // Invoice number generation
    String generateInvoiceNumber(Long companyId);
}
