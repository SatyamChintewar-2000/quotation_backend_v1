package com.satyam.quotation.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class QuotationDTO {
    private Long id;
    private String quotationNumber;
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private BigDecimal subtotal;
    private BigDecimal totalDiscount;
    private BigDecimal totalGst;
    private BigDecimal totalAmount;
    private String status;
    private LocalDate expiryDate;
    private LocalDate quotationDate;
    private String quotationCode;
    private LocalDate deliveryDate;
    private String executiveName;
    private String notes;
    private LocalDateTime createdAt;    private Long createdBy;
    private String createdByName;
    private List<QuotationItemDTO> items;
    private List<QuotationServiceDTO> services;

    // Feature 2: Toggle service charges on PDF
    private Boolean hideServiceChargesOnPdf;

    // Export rate snapshots — frozen at quotation creation time
    private java.math.BigDecimal usdExchangeRateSnapshot;
    private java.math.BigDecimal ratePerCbmSnapshot;

    private String customerAddress;
    private String shippingAddress;
}

