package com.satyam.quotation.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class QuotationItemDTO {
    private Long id;
    private Long productId;
    private String productName;
    private String productDescription;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal discountPercentage;
    private BigDecimal taxPercentage;
    private BigDecimal taxAmount;
    private BigDecimal itemTotal;
}

