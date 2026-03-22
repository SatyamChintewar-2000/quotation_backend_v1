package com.satyam.quotation.repository;

import com.satyam.quotation.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {
}
