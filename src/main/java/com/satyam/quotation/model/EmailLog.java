package com.satyam.quotation.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "quotation_id")
    private Quotation quotation;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Column(name = "email_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private EmailType emailType;

    @Column(name = "subject", length = 500)
    private String subject;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private EmailStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;

    public enum EmailType {
        QUOTATION_SENT,
        QUOTATION_APPROVED,
        QUOTATION_REJECTED,
        EXPIRY_WARNING,
        QUOTATION_UPDATED
    }

    public enum EmailStatus {
        SUCCESS,
        FAILED,
        PENDING
    }
}
