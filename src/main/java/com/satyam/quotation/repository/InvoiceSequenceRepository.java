package com.satyam.quotation.repository;

import com.satyam.quotation.model.InvoiceSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceSequenceRepository extends JpaRepository<InvoiceSequence, Long> {
    
    Optional<InvoiceSequence> findByCompanyIdAndYear(Long companyId, Integer year);
}
