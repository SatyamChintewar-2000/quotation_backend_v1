package com.satyam.quotation.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "enquiry")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Enquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "enquiry_date", nullable = false)
    private LocalDate enquiryDate;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String contact;

    private String email;
    private String gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Builder.Default
    private BigDecimal budget = BigDecimal.ZERO;

    private String address;

    @Column(name = "enquiry_for")
    private String enquiryFor;

    private String rating;

    @Column(nullable = false)
    @Builder.Default
    private String status = "open";

    private String city;

    @Column(name = "refer_type")
    private String referType;

    @Column(name = "refer_by")
    private String referBy;

    @Column(name = "next_followup_date")
    private LocalDate nextFollowupDate;

    private String comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "converted_customer_id")
    private Customer convertedCustomer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    private Long deletedBy;
}
