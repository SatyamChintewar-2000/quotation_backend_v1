package com.satyam.quotation.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quotation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quotation_number", unique = true)
    private String quotationNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "status")
    @Builder.Default
    private String status = "DRAFT";

    @Column(name = "currency", length = 3)
    @Builder.Default
    private String currency = "INR";

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "terms_and_conditions", columnDefinition = "TEXT")
    private String termsAndConditions;

    @Column(name = "total_amount")
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "subtotal")
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "total_discount")
    @Builder.Default
    private BigDecimal totalDiscount = BigDecimal.ZERO;

    @Column(name = "total_gst")
    @Builder.Default
    private BigDecimal totalGst = BigDecimal.ZERO;

    @Column(name = "discount_percentage")
    @Builder.Default
    private BigDecimal discountPercentage = BigDecimal.ZERO;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "quotation_date")
    private LocalDate quotationDate;

    @Column(name = "quotation_code")
    private String quotationCode;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @Column(name = "executive_name")
    private String executiveName;

    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<QuotationService> services = new ArrayList<>();

    @Column(name = "pdf_path")
    private String pdfPath;

    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<QuotationItem> items = new ArrayList<>();

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    private Long deletedBy;

    // Email tracking fields
    @Column(name = "email_sent")
    @Builder.Default
    private Boolean emailSent = false;

    @Column(name = "email_sent_at")
    private LocalDateTime emailSentAt;

    @Column(name = "email_status", length = 50)
    private String emailStatus;

    @Column(name = "email_error_message", columnDefinition = "TEXT")
    private String emailErrorMessage;

    @Column(name = "last_reminder_sent_at")
    private LocalDateTime lastReminderSentAt;
}
