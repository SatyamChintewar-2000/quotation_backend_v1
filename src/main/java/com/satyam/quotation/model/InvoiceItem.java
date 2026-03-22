package com.satyam.quotation.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@Entity
@Table(name = "invoice_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class InvoiceItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;
    
    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @Column(nullable = false)
    private String productName;
    
    @Column(columnDefinition = "TEXT")
    private String productDescription;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;
    
    @Column(precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discountPercentage = BigDecimal.ZERO;
    
    @Column(precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal taxPercentage = BigDecimal.ZERO;
    
    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;
    
    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal itemTotal = BigDecimal.ZERO;
    
    @Column(nullable = false, precision = 15, scale = 2)
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
        this.total = this.itemTotal;
    }
}
