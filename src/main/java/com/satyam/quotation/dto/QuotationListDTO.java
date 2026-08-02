package com.satyam.quotation.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Lightweight DTO for the quotation list/history page.
 *
 * Key differences from QuotationDTO:
 *  - Items contain NO imagePathSnapshot / imagePath (base64 images can be MBs each)
 *  - Items contain only the fields the list table needs: name, price, qty, totals
 *  - Services omitted entirely (not shown in the list table)
 *
 * Full detail (with images) is loaded on-demand via GET /api/quotations/{id}
 * when the user opens the View modal or downloads a PDF.
 */
@Data
public class QuotationListDTO {

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
    private java.time.LocalDate expiryDate;
    private java.time.LocalDate quotationDate;
    private String quotationCode;
    private java.time.LocalDate deliveryDate;
    private String executiveName;
    private String notes;
    private LocalDateTime createdAt;
    private Long createdBy;
    private Boolean hideServiceChargesOnPdf;

    // Items without images — only what the list table displays
    private List<QuotationListItemDTO> items;

    @Data
    public static class QuotationListItemDTO {
        private Long id;
        private Long productId;
        private String productName;        // from productNameSnapshot || product.productName
        private String productDescription; // from productDescriptionSnapshot
        private BigDecimal unitPrice;
        private Integer quantity;
        private BigDecimal discountPercentage;
        private BigDecimal taxPercentage;
        private BigDecimal itemTotal;
        // ── imagePathSnapshot intentionally excluded ──
    }
}
