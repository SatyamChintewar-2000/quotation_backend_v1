package com.satyam.quotation.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class InvoiceRequestDTO {
    private Long quotationId; // Optional - null for direct invoices
    private Long customerId; // Required for direct invoices
    private String customerName; // Optional - for direct invoices
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private BigDecimal discountPercentage;
    private String notes;
    private String termsAndConditions;
    private List<InvoiceItemDTO> items;
}
