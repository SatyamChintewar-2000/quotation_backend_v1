package com.satyam.quotation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class QuotationItemRequestDTO {
    
    @NotNull(message = "Product ID is required")
    private Long productId;
    
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;
    
    private BigDecimal unitPrice;
    
    private BigDecimal discountPercentage = BigDecimal.ZERO;
    
    private BigDecimal taxPercentage = BigDecimal.ZERO;
}
