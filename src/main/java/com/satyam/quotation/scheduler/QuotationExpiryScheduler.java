package com.satyam.quotation.scheduler;

import com.satyam.quotation.model.Quotation;
import com.satyam.quotation.repository.QuotationRepository;
import com.satyam.quotation.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class QuotationExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(QuotationExpiryScheduler.class);

    private final QuotationRepository quotationRepository;
    private final EmailService emailService;

    @Value("${app.scheduler.expiry-warning-days:3}")
    private int expiryWarningDays;

    public QuotationExpiryScheduler(QuotationRepository quotationRepository,
                                    EmailService emailService) {
        this.quotationRepository = quotationRepository;
        this.emailService = emailService;
    }

    /**
     * Check for quotations expiring soon and send reminder emails.
     * Runs daily at 9 AM.
     *
     * FIX: Previously called findAll() which loaded the entire quotations table
     * into JVM heap on every run — a memory spike that could crash the service
     * as data grows. Now delegates all filtering to the database via a targeted
     * JPQL query, so only the relevant rows are loaded.
     */
    @Scheduled(cron = "${app.scheduler.expiry-check-cron:0 0 9 * * ?}")
    public void checkExpiringQuotations() {
        log.info("Starting expiry check for quotations...");

        try {
            LocalDate warningDate = LocalDate.now().plusDays(expiryWarningDays);
            LocalDate today = LocalDate.now();
            // Cutoff: only send reminder if last one was > 24 hours ago
            LocalDateTime cutoff = LocalDateTime.now().minusHours(24);

            // All filtering pushed to DB — no more full table load into memory
            List<Quotation> quotations = quotationRepository
                    .findSentQuotationsExpiringSoon(today, warningDate, cutoff);

            log.info("Found {} quotations expiring within {} days", quotations.size(), expiryWarningDays);

            for (Quotation quotation : quotations) {
                try {
                    emailService.sendExpiryWarningEmail(quotation);
                    log.info("Sent expiry warning for quotation: {}", quotation.getQuotationNumber());
                } catch (Exception e) {
                    // Per-quotation failure must NOT abort the rest of the batch
                    log.error("Failed to send expiry warning for quotation: {}",
                            quotation.getQuotationNumber(), e);
                }
            }

            log.info("Expiry check completed successfully");

        } catch (Exception e) {
            // Top-level catch: scheduler must never throw — an uncaught exception
            // from a @Scheduled method suppresses all future executions of that job
            log.error("Expiry check failed with unexpected error", e);
        }
    }
}
