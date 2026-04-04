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

    private LocalDate expiryDate;

    private LocalDate quotationDate;

    private String quotationCode;

    private LocalDate deliveryDate;

    private String executiveName;

    private String status = "DRAFT";

    private String currency = "INR";

    private String notes;

    private String termsAndConditions;

    @NotNull(message = "Items are required")
    private List<QuotationItemRequestDTO> items;

    private List<QuotationServiceDTO> services;
}
