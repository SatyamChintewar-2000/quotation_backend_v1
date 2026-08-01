package com.satyam.quotation.repository;

import com.satyam.quotation.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.company WHERE p.company.id = :companyId AND p.active = true")
    List<Product> findByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.company WHERE p.createdBy = :userId AND p.active = true")
    List<Product> findByCreatedBy(@Param("userId") Long userId);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.company WHERE p.active = true")
    List<Product> findAllActive();

    long countByCompanyIdAndActiveTrue(Long companyId);

    long countByCreatedByAndActiveTrue(Long createdBy);
}
