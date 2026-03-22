package com.satyam.quotation.repository;

import com.satyam.quotation.model.Quotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface QuotationRepository extends JpaRepository<Quotation, Long> {

    List<Quotation> findByCompanyId(Long companyId);

    List<Quotation> findByCreatedBy(Long userId);

    @Query("SELECT SUM(q.totalAmount) FROM Quotation q WHERE q.company.id = :companyId")
    Double getTotalRevenueByCompany(Long companyId);

    @Query("SELECT SUM(q.totalAmount) FROM Quotation q WHERE q.createdBy = :userId")
    Double getTotalRevenueByUser(Long userId);
}
