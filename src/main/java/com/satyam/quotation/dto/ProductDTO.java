package com.satyam.quotation.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ProductDTO {
    private Long id;
    private String productName;
    private String description;
    private BigDecimal price;
    private String unit;
    private Integer quantity;
    private BigDecimal discountPercentage;
    private String taxType;
    private BigDecimal taxPercentage;
    private LocalDate expiryDate;
    private String imagePath;
    private Long companyId;
    private String companyName;
    private Long createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
    private Boolean active;
}
