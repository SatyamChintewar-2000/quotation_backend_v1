package com.satyam.quotation.repository;

import com.satyam.quotation.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    
    List<Invoice> findByCompanyId(Long companyId);
    
    List<Invoice> findByCustomerId(Long customerId);
    
    List<Invoice> findByQuotationId(Long quotationId);
    
    List<Invoice> findByStatus(String status);
    
    List<Invoice> findByPaymentStatus(String paymentStatus);
    
    List<Invoice> findByCompanyIdAndStatus(Long companyId, String status);
    
    List<Invoice> findByCompanyIdAndPaymentStatus(Long companyId, String paymentStatus);
    
    @Query("SELECT i FROM Invoice i WHERE i.company.id = :companyId AND i.active = true ORDER BY i.invoiceDate DESC")
    List<Invoice> findActiveInvoicesByCompany(@Param("companyId") Long companyId);
    
    @Query("SELECT i FROM Invoice i WHERE i.company.id = :companyId AND i.status = :status AND i.active = true")
    List<Invoice> findActiveInvoicesByCompanyAndStatus(@Param("companyId") Long companyId, @Param("status") String status);
    
    @Query("SELECT i FROM Invoice i WHERE i.company.id = :companyId AND i.paymentStatus = :paymentStatus AND i.active = true")
    List<Invoice> findActiveInvoicesByCompanyAndPaymentStatus(@Param("companyId") Long companyId, @Param("paymentStatus") String paymentStatus);
    
    @Query("SELECT i FROM Invoice i WHERE i.company.id = :companyId AND i.invoiceDate BETWEEN :startDate AND :endDate AND i.active = true")
    List<Invoice> findInvoicesByDateRange(@Param("companyId") Long companyId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT i FROM Invoice i WHERE i.company.id = :companyId AND i.dueDate < CURRENT_DATE AND i.paymentStatus != 'PAID' AND i.active = true")
    List<Invoice> findOverdueInvoices(@Param("companyId") Long companyId);
}
