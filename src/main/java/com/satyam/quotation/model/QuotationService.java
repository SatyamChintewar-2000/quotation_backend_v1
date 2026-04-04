package com.satyam.quotation.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "quotation_service")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class QuotationService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_id", nullable = false)
    private Quotation quotation;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Column(name = "service_price")
    @Builder.Default
    private BigDecimal servicePrice = BigDecimal.ZERO;

    @Column(name = "service_tax")
    @Builder.Default
    private BigDecimal serviceTax = BigDecimal.ZERO;
}
