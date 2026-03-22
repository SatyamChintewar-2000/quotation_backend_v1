package com.satyam.quotation.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class QuotationDTO {
    private Long id;
    private String quotationNumber;
    private Long customerId;
    private String customerName;
    private BigDecimal subtotal;
    private BigDecimal totalDiscount;
    private BigDecimal totalGst;
    private BigDecimal totalAmount;
    private String status;
    private java.time.LocalDate expiryDate;
    private java.time.LocalDateTime createdAt;
    private Long createdBy;
    private String createdByName;
    private List<QuotationItemDTO> items;
}

