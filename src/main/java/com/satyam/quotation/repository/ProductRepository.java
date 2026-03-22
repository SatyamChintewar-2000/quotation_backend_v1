package com.satyam.quotation.repository;

import com.satyam.quotation.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCompanyId(Long companyId);
}
