package com.satyam.quotation.repository;

import com.satyam.quotation.model.Company;
import com.satyam.quotation.model.QuotationSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface QuotationSequenceRepository extends JpaRepository<QuotationSequence, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT qs FROM QuotationSequence qs WHERE qs.company = :company AND qs.year = :year")
    Optional<QuotationSequence> findByCompanyAndYearForUpdate(
            @Param("company") Company company,
            @Param("year") Integer year);
    
    Optional<QuotationSequence> findByCompanyAndYear(Company company, Integer year);
}
