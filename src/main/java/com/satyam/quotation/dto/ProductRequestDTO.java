package com.satyam.quotation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ProductRequestDTO {
    
    @NotBlank(message = "Product name is required")
    private String productName;

    private String hsnSacCode;

    private String productCode;

    private String brand;

    private String category;

    private String description;

    // Optional: used by SUPER_ADMIN to specify which company the product belongs to
    private Long companyId;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private BigDecimal price;

    private BigDecimal purchasePrice = BigDecimal.ZERO;
    
    private String unit = "piece";
    
    @NotNull(message = "Quantity is required")
    private Integer quantity = 0;
    
    private BigDecimal discountPercentage = BigDecimal.ZERO;
    
    private String taxType = "GST";
    
    private BigDecimal taxPercentage = BigDecimal.ZERO;

    private String hsnCode;
    
    private LocalDate expiryDate;
    
    private String imagePath;

    // Optional: weight and volume per unit (for export/logistics quotations)
    private java.math.BigDecimal netWeight;
    private java.math.BigDecimal cbm;
}
