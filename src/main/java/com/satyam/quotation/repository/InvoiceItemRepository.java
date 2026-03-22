package com.satyam.quotation.repository;

import com.satyam.quotation.model.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {
    
    List<InvoiceItem> findByInvoiceId(Long invoiceId);
    
    List<InvoiceItem> findByProductId(Long productId);
}
