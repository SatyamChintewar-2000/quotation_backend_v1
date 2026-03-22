package com.satyam.quotation.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PaymentDTO {
    private Long id;
    private Long invoiceId;
    private LocalDate paymentDate;
    private BigDecimal paymentAmount;
    private String paymentMethod;
    private String paymentReference;
    private String notes;
    private Long createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
}
