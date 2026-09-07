package com.satyam.quotation.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "product_code")
    private String productCode;

    @Column(name = "hsn_sac_code", length = 20)
    private String hsnSacCode;

    private String brand;

    private String category;

    private String description;

    private BigDecimal price;

    @Column(name = "purchase_price")
    private BigDecimal purchasePrice = BigDecimal.ZERO;

    @Column(name = "tax_percentage")
    private BigDecimal taxPercentage = BigDecimal.ZERO;

    @Column(name = "unit")
    private String unit = "piece";

    @Column(name = "quantity")
    private Integer quantity = 0;

    @Column(name = "discount_percentage")
    private BigDecimal discountPercentage = BigDecimal.ZERO;

    @Column(name = "tax_type")
    private String taxType = "GST";

    @Column(name = "hsn_code", length = 20)
    private String hsnCode;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "image_path")
    private String imagePath;

    // Optional fields for manufacturers and exporters
    @Column(name = "net_weight", precision = 10, scale = 3)
    private BigDecimal netWeight;   // net weight in kg per unit

    @Column(name = "stack_weight", precision = 10, scale = 3)
    private BigDecimal stackWeight; // gross/stack weight in kg per unit (including packaging)

    @Column(name = "cbm", precision = 10, scale = 4)
    private BigDecimal cbm;         // volume in cubic metres per unit

    // ── International Purchase (USD) fields ───────────────────────────────────
    // 'INR' or 'USD' — determines which purchase tab was used
    @Column(name = "purchase_price_currency", length = 3)
    @Builder.Default
    private String purchasePriceCurrency = "INR";

    // Purchase price per unit in USD (entered directly by user)
    @Column(name = "purchase_price_usd", precision = 12, scale = 4)
    private BigDecimal purchasePriceUsd;

    // Shipping cost per unit in USD (item's share of total shipping)
    @Column(name = "shipping_cost_usd", precision = 10, scale = 4)
    private BigDecimal shippingCostUsd;

    // GST + Customs duty rate (default 31%) applied on (item USD + shipping USD)
    @Column(name = "duty_gst_percent", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal dutyGstPercent = new BigDecimal("31.00");

    // Clearance cost per unit in INR (port / documentation / agent fees)
    @Column(name = "clearance_cost", precision = 12, scale = 2)
    private BigDecimal clearanceCost;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    private Long deletedBy;
}
