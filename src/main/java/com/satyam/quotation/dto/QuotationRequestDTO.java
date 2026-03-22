package com.satyam.quotation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class QuotationRequestDTO {
    
    @NotNull(message = "Customer ID is required")
    private Long customerId;
    
    private BigDecimal discountPercentage = BigDecimal.ZERO;
    
    @NotNull(message = "Expiry date is required")
    private LocalDate expiryDate;
    
    private String status = "DRAFT";
    
    private String currency = "INR";
    
    private String notes;
    
    private String termsAndConditions;
    
    @NotNull(message = "Items are required")
    private List<QuotationItemRequestDTO> items;
}
