package com.satyam.quotation.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class QuotationItemDTO {
    private Long id;
    private Long productId;
    private String productName;
    private String productDescription;
    private String productNameSnapshot;
    private String productDescriptionSnapshot;
    private String imagePath;  // Product image (from current product)
    private String imagePathSnapshot;  // Product image snapshot (at quotation time)
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal discountPercentage;
    private BigDecimal taxPercentage;
    private BigDecimal taxAmount;
    private BigDecimal itemTotal;

    // Weight & CBM snapshots — captured at time of quoting
    private BigDecimal netWeightSnapshot;
    private BigDecimal cbmSnapshot;
}

