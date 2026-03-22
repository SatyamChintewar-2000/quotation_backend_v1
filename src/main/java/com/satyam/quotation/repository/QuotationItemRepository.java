package com.satyam.quotation.repository;

import com.satyam.quotation.model.QuotationItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuotationItemRepository extends JpaRepository<QuotationItem, Long> {

    List<QuotationItem> findByQuotation_Id(Long quotationId);
}
