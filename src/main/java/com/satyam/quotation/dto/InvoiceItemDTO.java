package com.satyam.quotation.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class InvoiceItemDTO {
    private Long id;
    private Long invoiceId;
    private Long productId;
    private String itemType;   // "PRODUCT" | "SERVICE"
    private String productName;
    private String productDescription;
    private String hsnCode;
    private String imagePath;  // Product image — loaded from product at DTO conversion time
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal discountPercentage;
    private BigDecimal discountAmount;   // exact flat discount — avoids % rounding
    private BigDecimal taxPercentage;
    private BigDecimal taxAmount;
    private BigDecimal itemTotal;
    private BigDecimal total;
}
