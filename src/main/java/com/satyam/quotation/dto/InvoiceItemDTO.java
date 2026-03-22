package com.satyam.quotation.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class InvoiceItemDTO {
    private Long id;
    private Long invoiceId;
    private Long productId;
    private String productName;
    private String productDescription;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal discountPercentage;
    private BigDecimal taxPercentage;
    private BigDecimal taxAmount;
    private BigDecimal itemTotal;
    private BigDecimal total;
}
