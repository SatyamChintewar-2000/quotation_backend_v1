package com.satyam.quotation.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Lightweight product projection returned by GET /api/products (the list endpoint).
 *
 * WHY THIS EXISTS
 * ---------------
 * The full ProductDTO carries imagePath — a base64-encoded JPEG that can be 200 KB–2 MB
 * per product.  With 300–400 products per company that blows the list response up to
 * 60 MB–800 MB of JSON, which is the root cause of the 5-10 minute page-load time
 * experienced by clients on slower connections (e.g. Miraj/Sangli).
 *
 * This DTO omits imagePath completely.  Images are fetched on-demand via
 * GET /api/products/{id}/image only when the user opens the edit modal in
 * ProductManagement — one request per product, only when needed.
 *
 * FIELDS INCLUDED
 * ---------------
 * Every field that NewQuotation, DirectInvoice, QuotationHistory, and other
 * downstream pages need to render the product selector, do price calculations,
 * and build quotation/invoice payloads.  Nothing more.
 */
@Data
public class ProductListDTO {

    // Identity
    private Long   id;
    private String productName;
    private String productCode;
    private String hsnSacCode;
    private String hsnCode;
    private String brand;
    private String category;
    private String description;

    // Pricing
    private BigDecimal price;
    private BigDecimal purchasePrice;
    private BigDecimal discountPercentage;

    // Tax
    private String     taxType;
    private BigDecimal taxPercentage;

    // Logistics / physical
    private String     unit;
    private Integer    quantity;
    private LocalDate  expiryDate;
    private BigDecimal netWeight;
    private BigDecimal stackWeight;
    private BigDecimal cbm;

    // Company reference (needed by SUPER_ADMIN views)
    private Long   companyId;
    private String companyName;

    // International purchase (USD) fields — needed for CBM cost analysis panel
    private String     purchasePriceCurrency;
    private BigDecimal purchasePriceUsd;
    private BigDecimal shippingCostUsd;
    private BigDecimal dutyGstPercent;
    private BigDecimal clearanceCost;
    private BigDecimal domesticShippingInr;

    // NOTE: imagePath is intentionally omitted from this DTO.
    // Use GET /api/products/{id}/image to fetch the image for a specific product.
}
