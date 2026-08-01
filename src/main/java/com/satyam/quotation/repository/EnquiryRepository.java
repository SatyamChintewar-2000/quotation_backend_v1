package com.satyam.quotation.repository;

import com.satyam.quotation.model.Enquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EnquiryRepository extends JpaRepository<Enquiry, Long> {

    List<Enquiry> findByCompanyIdAndDeletedAtIsNull(Long companyId);

    List<Enquiry> findByCreatedByAndDeletedAtIsNull(Long createdBy);

    @Query("SELECT e FROM Enquiry e WHERE e.deletedAt IS NULL")
    List<Enquiry> findAllActive();

    @Query("SELECT COUNT(e) FROM Enquiry e WHERE e.createdBy = :userId AND e.deletedAt IS NULL")
    long countByCreatedByAndDeletedAtIsNull(@Param("userId") Long userId);

    @Query("SELECT COUNT(e) FROM Enquiry e WHERE e.company.id = :companyId AND e.deletedAt IS NULL")
    long countByCompanyIdAndDeletedAtIsNull(@Param("companyId") Long companyId);
}
