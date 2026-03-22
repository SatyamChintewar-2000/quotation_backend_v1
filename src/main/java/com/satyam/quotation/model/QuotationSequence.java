package com.satyam.quotation.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "quotation_sequence", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"company_id", "year"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuotationSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "last_sequence", nullable = false)
    @Builder.Default
    private Integer lastSequence = 0;

    @Column(name = "year", nullable = false)
    private Integer year;
}
