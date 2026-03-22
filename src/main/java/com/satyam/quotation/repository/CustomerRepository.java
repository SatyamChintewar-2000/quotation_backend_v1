package com.satyam.quotation.repository;

import com.satyam.quotation.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByCompanyId(Long companyId);

    List<Customer> findByCreatedBy(Long userId);
}
