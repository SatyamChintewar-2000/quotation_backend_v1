package com.satyam.quotation.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ProductDTO {
    private Long id;
    private String productName;
    private String productCode;
    private String hsnSacCode;
    private String brand;
    private String category;
    private String description;
    private BigDecimal price;
    private BigDecimal purchasePrice;
    private String unit;
    private Integer quantity;
    private BigDecimal discountPercentage;
    private String taxType;
    private BigDecimal taxPercentage;
    private String hsnCode;
    private LocalDate expiryDate;
    private String imagePath;
    private Long companyId;
    private String companyName;
    private Long createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
    private Boolean active;

    // Optional: weight and volume per unit (for export/logistics quotations)
    private java.math.BigDecimal netWeight;
    private java.math.BigDecimal stackWeight; // gross/stack weight in kg per unit
    private java.math.BigDecimal cbm;

    // International Purchase (USD) fields
    private String purchasePriceCurrency;      // "INR" or "USD"
    private java.math.BigDecimal purchasePriceUsd;   // per-unit price in USD
    private java.math.BigDecimal shippingCostUsd;    // per-unit shipping in USD
    private java.math.BigDecimal dutyGstPercent;     // GST+duty % (default 31)
    private java.math.BigDecimal clearanceCost;      // clearance cost in INR per unit
}
