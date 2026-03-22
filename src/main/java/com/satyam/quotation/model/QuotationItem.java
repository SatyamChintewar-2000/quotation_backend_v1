package com.satyam.quotation.model;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "quotation_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class QuotationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "quotation_id")
    private Quotation quotation;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    // Product snapshot fields (captured at time of quoting)
    @Column(name = "product_name_snapshot")
    private String productNameSnapshot;

    @Column(name = "product_description_snapshot", columnDefinition = "TEXT")
    private String productDescriptionSnapshot;

    @Column(name = "unit_snapshot", length = 50)
    private String unitSnapshot;

    // Legacy fields (kept for backward compatibility)
    @Column(name = "product_name")
    private String productName;

    @Column(name = "product_description")
    private String productDescription;

    private Integer quantity;

    @Column(name = "unit_price", precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "discount_percentage", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discountPercentage = BigDecimal.ZERO;

    // Legacy field
    @Column(name = "item_discount_percentage", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal itemDiscountPercentage = BigDecimal.ZERO;

    @Column(name = "tax_percentage", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal taxPercentage = BigDecimal.ZERO;

    @Column(name = "tax_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "item_total", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal itemTotal = BigDecimal.ZERO;

    // Legacy field
    private BigDecimal total;

    /**
     * Calculate item total
     * Formula: (unitPrice × quantity) × (1 - discount/100) + taxAmount
     */
    public void calculateItemTotal() {
        if (unitPrice == null || quantity == null) {
            this.itemTotal = BigDecimal.ZERO;
            this.total = BigDecimal.ZERO;
            return;
        }

        // Base amount
        BigDecimal baseAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));

        // Apply discount
        BigDecimal discountAmount = baseAmount.multiply(discountPercentage).divide(BigDecimal.valueOf(100));
        BigDecimal amountAfterDiscount = baseAmount.subtract(discountAmount);

        // Calculate tax
        BigDecimal taxAmt = amountAfterDiscount.multiply(taxPercentage).divide(BigDecimal.valueOf(100));
        this.taxAmount = taxAmt;

        // Final total
        this.itemTotal = amountAfterDiscount.add(taxAmt);
        this.total = this.itemTotal; // For backward compatibility
    }
}
