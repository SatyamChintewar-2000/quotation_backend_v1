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
     * Check for quotations expiring soon and send reminder emails
     * Runs daily at 9 AM
     */
    @Scheduled(cron = "${app.scheduler.expiry-check-cron:0 0 9 * * ?}")
    public void checkExpiringQuotations() {
        log.info("Starting expiry check for quotations...");

        LocalDate warningDate = LocalDate.now().plusDays(expiryWarningDays);
        LocalDate today = LocalDate.now();

        // Find quotations that are:
        // 1. In SENT status
        // 2. Expiring within warning days
        // 3. Haven't received a reminder in the last 24 hours (or never)
        List<Quotation> quotations = quotationRepository.findAll().stream()
                .filter(q -> "SENT".equals(q.getStatus()))
                .filter(q -> q.getExpiryDate() != null)
                .filter(q -> !q.getExpiryDate().isBefore(today)) // Not already expired
                .filter(q -> !q.getExpiryDate().isAfter(warningDate)) // Expiring soon
                .filter(q -> shouldSendReminder(q))
                .toList();

        log.info("Found {} quotations expiring within {} days", quotations.size(), expiryWarningDays);

        for (Quotation quotation : quotations) {
            try {
                emailService.sendExpiryWarningEmail(quotation);
                log.info("Sent expiry warning for quotation: {}", quotation.getQuotationNumber());
            } catch (Exception e) {
                log.error("Failed to send expiry warning for quotation: {}",
                        quotation.getQuotationNumber(), e);
            }
        }

        log.info("Expiry check completed");
    }

    private boolean shouldSendReminder(Quotation quotation) {
        if (quotation.getLastReminderSentAt() == null) {
            return true; // Never sent a reminder
        }

        // Send reminder only if last one was sent more than 24 hours ago
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
        return quotation.getLastReminderSentAt().isBefore(twentyFourHoursAgo);
    }
}
