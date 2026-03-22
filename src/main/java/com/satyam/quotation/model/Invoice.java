package com.satyam.quotation.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoice")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String invoiceNumber;
    
    @ManyToOne
    @JoinColumn(name = "quotation_id", nullable = false)
    private Quotation quotation;
    
    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    
    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
    
    @Column(nullable = false)
    private LocalDate invoiceDate;
    
    @Column(nullable = false)
    private LocalDate dueDate;
    
    @Column(precision = 15, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;
    
    @Column(precision = 5, scale = 2)
    private BigDecimal discountPercentage = BigDecimal.ZERO;
    
    @Column(precision = 15, scale = 2)
    private BigDecimal totalDiscount = BigDecimal.ZERO;
    
    @Column(precision = 15, scale = 2)
    private BigDecimal totalTax = BigDecimal.ZERO;
    
    @Column(precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;
    
    @Column(length = 20)
    private String status = "DRAFT"; // DRAFT, SENT, PARTIAL, PAID, OVERDUE, CANCELLED
    
    @Column(length = 20)
    private String paymentStatus = "PENDING"; // PENDING, PARTIAL, PAID
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @Column(columnDefinition = "TEXT")
    private String termsAndConditions;
    
    @Column(nullable = false)
    private Boolean emailSent = false;
    
    @Column
    private LocalDateTime emailSentAt;
    
    @Column(nullable = false)
    private Long createdBy;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column
    private Long updatedBy;
    
    @Column
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @Column(nullable = false)
    private Boolean active = true;
    
    @Column
    private LocalDateTime deletedAt;
    
    @Column
    private Long deletedBy;
}
