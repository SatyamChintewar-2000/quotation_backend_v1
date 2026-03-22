package com.satyam.quotation.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_name")
    private String productName;

    private String description;

    private BigDecimal price;

    @Column(name = "tax_percentage")
    private BigDecimal taxPercentage = BigDecimal.ZERO;

    @Column(name = "unit")
    private String unit = "piece";

    @Column(name = "quantity")
    private Integer quantity = 0;

    @Column(name = "discount_percentage")
    private BigDecimal discountPercentage = BigDecimal.ZERO;

    @Column(name = "tax_type")
    private String taxType = "GST";

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "image_path")
    private String imagePath;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    private Long deletedBy;
}
