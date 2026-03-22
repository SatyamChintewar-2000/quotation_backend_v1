package com.satyam.quotation.repository;

import com.satyam.quotation.model.InvoicePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface InvoicePaymentRepository extends JpaRepository<InvoicePayment, Long> {
    
    List<InvoicePayment> findByInvoiceId(Long invoiceId);
    
    @Query("SELECT COALESCE(SUM(p.paymentAmount), 0) FROM InvoicePayment p WHERE p.invoice.id = :invoiceId")
    BigDecimal getTotalPaymentsByInvoiceId(@Param("invoiceId") Long invoiceId);
}
