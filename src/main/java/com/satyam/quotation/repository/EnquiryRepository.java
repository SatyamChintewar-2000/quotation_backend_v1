package com.satyam.quotation.repository;

import com.satyam.quotation.model.Enquiry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnquiryRepository extends JpaRepository<Enquiry, Long> {
    List<Enquiry> findByCompanyIdAndDeletedAtIsNull(Long companyId);
    List<Enquiry> findByCreatedByAndDeletedAtIsNull(Long createdBy);
}
