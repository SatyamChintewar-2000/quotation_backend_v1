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
    
    /**
     * Optional flat discount amount. When provided and > 0, it means the user
     * typed a rupee amount instead of a percentage. The frontend converts it to
     * discountPercentage before sending so all backend calculations stay
     * percentage-based; this field is stored for PDF display only.
     */
    private BigDecimal discountAmount;

    private BigDecimal taxPercentage = BigDecimal.ZERO;
}
