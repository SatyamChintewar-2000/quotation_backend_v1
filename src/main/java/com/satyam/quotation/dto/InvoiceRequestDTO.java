package com.satyam.quotation.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class InvoiceRequestDTO {
    private Long quotationId;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private BigDecimal discountPercentage;
    private String notes;
    private String termsAndConditions;
    private List<InvoiceItemDTO> items;
}
