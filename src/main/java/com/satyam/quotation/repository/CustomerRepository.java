package com.satyam.quotation.repository;

import com.satyam.quotation.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    @Query("SELECT c FROM Customer c LEFT JOIN FETCH c.company WHERE c.company.id = :companyId AND c.active = true")
    List<Customer> findByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT c FROM Customer c LEFT JOIN FETCH c.company WHERE c.createdBy = :userId AND c.active = true")
    List<Customer> findByCreatedBy(@Param("userId") Long userId);

    long countByCompanyIdAndActiveTrue(Long companyId);

    long countByCreatedByAndActiveTrue(Long createdBy);
}
