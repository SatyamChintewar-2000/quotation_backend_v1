package com.satyam.quotation.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class InvoiceDTO {
    private Long id;
    private String invoiceNumber;
    private Long quotationId;
    private Long customerId;
    private String customerName;
    private Long companyId;
    private String companyName;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private BigDecimal subtotal;
    private BigDecimal discountPercentage;
    private BigDecimal totalDiscount;
    private BigDecimal totalTax;
    private BigDecimal totalAmount;
    private String status;
    private String paymentStatus;
    private String notes;
    private String termsAndConditions;
    private Boolean emailSent;
    private LocalDateTime emailSentAt;
    private Long createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    private Boolean active;
    private List<InvoiceItemDTO> items;
    private List<PaymentDTO> payments;
    private BigDecimal totalPaid;
    private BigDecimal remainingBalance;
}
