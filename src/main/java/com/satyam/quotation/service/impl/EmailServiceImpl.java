package com.satyam.quotation.service.impl;

import com.satyam.quotation.model.EmailLog;
import com.satyam.quotation.model.Quotation;
import com.satyam.quotation.model.User;
import com.satyam.quotation.repository.EmailLogRepository;
import com.satyam.quotation.repository.QuotationRepository;
import com.satyam.quotation.service.AppSettingsService;
import com.satyam.quotation.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;
    private final EmailLogRepository emailLogRepository;
    private final QuotationRepository quotationRepository;
    private final com.satyam.quotation.repository.UserRepository userRepository;
    private final AppSettingsService appSettingsService;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Value("${app.mail.enabled:true}")
    private boolean emailEnabledConfig;

    public EmailServiceImpl(JavaMailSender mailSender,
                           EmailLogRepository emailLogRepository,
                           QuotationRepository quotationRepository,
                           com.satyam.quotation.repository.UserRepository userRepository,
                           AppSettingsService appSettingsService) {
        this.mailSender = mailSender;
        this.emailLogRepository = emailLogRepository;
        this.quotationRepository = quotationRepository;
        this.userRepository = userRepository;
        this.appSettingsService = appSettingsService;
    }

    private boolean isEmailEnabled() {
        // DB setting takes precedence over config file
        return emailEnabledConfig && appSettingsService.getBooleanSetting("email_notifications_enabled", true);
    }

    private String getCreatorName(Quotation quotation) {
        if (quotation.getCreatedBy() == null) {
            return "Unknown";
        }
        return userRepository.findById(quotation.getCreatedBy())
                .map(User::getName)
                .orElse("Unknown");
    }

    @Override
    public EmailLog sendQuotationEmail(Quotation quotation, File pdfFile, User sentBy) {
        String recipientEmail = quotation.getCustomer().getEmail();
        String subject = String.format("Quotation %s from %s",
                quotation.getQuotationNumber(),
                quotation.getCompany().getCompanyName());

        String body = buildQuotationEmailBody(quotation);

        EmailLog emailLog = EmailLog.builder()
                .quotation(quotation)
                .recipientEmail(recipientEmail)
                .emailType(EmailLog.EmailType.QUOTATION_SENT)
                .subject(subject)
                .body(body)
                .status(EmailLog.EmailStatus.PENDING)
                .createdBy(sentBy)
                .build();

        try {
            if (isEmailEnabled()) {
                sendEmailWithAttachment(recipientEmail, subject, body, pdfFile);
                emailLog.setStatus(EmailLog.EmailStatus.SUCCESS);
                emailLog.setSentAt(LocalDateTime.now());

                // Update quotation
                quotation.setEmailSent(true);
                quotation.setEmailSentAt(LocalDateTime.now());
                quotation.setEmailStatus("SUCCESS");
                quotationRepository.save(quotation);

                log.info("Quotation email sent successfully to: {}", recipientEmail);
            } else {
                log.warn("Email sending is disabled. Email would be sent to: {}", recipientEmail);
                emailLog.setStatus(EmailLog.EmailStatus.SUCCESS);
                emailLog.setSentAt(LocalDateTime.now());
            }
        } catch (Exception e) {
            log.error("Failed to send quotation email to: {}", recipientEmail, e);
            emailLog.setStatus(EmailLog.EmailStatus.FAILED);
            emailLog.setErrorMessage(e.getMessage());

            // Update quotation
            quotation.setEmailSent(false);
            quotation.setEmailStatus("FAILED");
            quotation.setEmailErrorMessage(e.getMessage());
            quotationRepository.save(quotation);
        }

        return emailLogRepository.save(emailLog);
    }

    @Override
    public EmailLog sendQuotationApprovedEmail(Quotation quotation) {
        User creator = userRepository.findById(quotation.getCreatedBy())
                .orElseThrow(() -> new RuntimeException("Creator user not found"));
        String recipientEmail = creator.getEmail();
        String subject = String.format("Quotation %s Approved", quotation.getQuotationNumber());
        String body = buildApprovedEmailBody(quotation);

        return sendNotificationEmail(quotation, recipientEmail, subject, body,
                EmailLog.EmailType.QUOTATION_APPROVED, creator);
    }

    @Override
    public EmailLog sendQuotationRejectedEmail(Quotation quotation) {
        User creator = userRepository.findById(quotation.getCreatedBy())
                .orElseThrow(() -> new RuntimeException("Creator user not found"));
        String recipientEmail = creator.getEmail();
        String subject = String.format("Quotation %s Rejected", quotation.getQuotationNumber());
        String body = buildRejectedEmailBody(quotation);

        return sendNotificationEmail(quotation, recipientEmail, subject, body,
                EmailLog.EmailType.QUOTATION_REJECTED, creator);
    }

    @Override
    public EmailLog sendExpiryWarningEmail(Quotation quotation) {
        User creator = userRepository.findById(quotation.getCreatedBy())
                .orElseThrow(() -> new RuntimeException("Creator user not found"));
        String recipientEmail = creator.getEmail();
        String subject = String.format("Quotation %s Expiring Soon", quotation.getQuotationNumber());
        String body = buildExpiryWarningEmailBody(quotation);

        EmailLog emailLog = sendNotificationEmail(quotation, recipientEmail, subject, body,
                EmailLog.EmailType.EXPIRY_WARNING, creator);

        // Update last reminder sent time
        quotation.setLastReminderSentAt(LocalDateTime.now());
        quotationRepository.save(quotation);

        return emailLog;
    }

    @Override
    public EmailLog sendQuotationUpdatedEmail(Quotation quotation, User updatedBy) {
        String recipientEmail = quotation.getCustomer().getEmail();
        String subject = String.format("Quotation %s Updated", quotation.getQuotationNumber());
        String body = buildUpdatedEmailBody(quotation);

        return sendNotificationEmail(quotation, recipientEmail, subject, body,
                EmailLog.EmailType.QUOTATION_UPDATED, updatedBy);
    }

    @Override
    public EmailLog retryFailedEmail(Long emailLogId) {
        EmailLog emailLog = emailLogRepository.findById(emailLogId)
                .orElseThrow(() -> new RuntimeException("Email log not found"));

        if (emailLog.getStatus() != EmailLog.EmailStatus.FAILED) {
            throw new RuntimeException("Can only retry failed emails");
        }

        try {
            if (isEmailEnabled()) {
                sendEmail(emailLog.getRecipientEmail(), emailLog.getSubject(), emailLog.getBody());
                emailLog.setStatus(EmailLog.EmailStatus.SUCCESS);
                emailLog.setSentAt(LocalDateTime.now());
                emailLog.setErrorMessage(null);
                log.info("Retry successful for email to: {}", emailLog.getRecipientEmail());
            }
        } catch (Exception e) {
            log.error("Retry failed for email to: {}", emailLog.getRecipientEmail(), e);
            emailLog.setErrorMessage(e.getMessage());
        }

        return emailLogRepository.save(emailLog);
    }

    private EmailLog sendNotificationEmail(Quotation quotation, String recipientEmail,
                                          String subject, String body,
                                          EmailLog.EmailType emailType, User createdBy) {
        EmailLog emailLog = EmailLog.builder()
                .quotation(quotation)
                .recipientEmail(recipientEmail)
                .emailType(emailType)
                .subject(subject)
                .body(body)
                .status(EmailLog.EmailStatus.PENDING)
                .createdBy(createdBy)
                .build();

        try {
            if (isEmailEnabled()) {
                sendEmail(recipientEmail, subject, body);
                emailLog.setStatus(EmailLog.EmailStatus.SUCCESS);
                emailLog.setSentAt(LocalDateTime.now());
                log.info("{} email sent successfully to: {}", emailType, recipientEmail);
            } else {
                log.warn("Email sending is disabled. {} email would be sent to: {}",
                        emailType, recipientEmail);
                emailLog.setStatus(EmailLog.EmailStatus.SUCCESS);
                emailLog.setSentAt(LocalDateTime.now());
            }
        } catch (Exception e) {
            log.error("Failed to send {} email to: {}", emailType, recipientEmail, e);
            emailLog.setStatus(EmailLog.EmailStatus.FAILED);
            emailLog.setErrorMessage(e.getMessage());
        }

        return emailLogRepository.save(emailLog);
    }

    private void sendEmail(String to, String subject, String body) throws MessagingException {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true); // true = HTML

            mailSender.send(message);
        } catch (Exception e) {
            throw new MessagingException("Failed to send email", e);
        }
    }

    private void sendEmailWithAttachment(String to, String subject, String body, File attachment)
            throws MessagingException {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true); // true = HTML

            if (attachment != null && attachment.exists()) {
                FileSystemResource file = new FileSystemResource(attachment);
                helper.addAttachment(attachment.getName(), file);
            }

            mailSender.send(message);
        } catch (Exception e) {
            throw new MessagingException("Failed to send email with attachment", e);
        }
    }

    private String buildQuotationEmailBody(Quotation quotation) {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");

        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }
                        .content { padding: 20px; background-color: #f9f9f9; }
                        .details { background-color: white; padding: 15px; margin: 15px 0; border-radius: 5px; }
                        .detail-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #eee; }
                        .detail-label { font-weight: bold; color: #666; }
                        .detail-value { color: #333; }
                        .amount { font-size: 24px; color: #4CAF50; font-weight: bold; }
                        .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                        .button { background-color: #4CAF50; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block; margin: 15px 0; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>%s</h1>
                            <p>Quotation from %s</p>
                        </div>
                        <div class="content">
                            <p>Dear %s,</p>
                            <p>Thank you for your interest. Please find attached the quotation for your review.</p>
                            
                            <div class="details">
                                <div class="detail-row">
                                    <span class="detail-label">Quotation Number:</span>
                                    <span class="detail-value">%s</span>
                                </div>
                                <div class="detail-row">
                                    <span class="detail-label">Date:</span>
                                    <span class="detail-value">%s</span>
                                </div>
                                <div class="detail-row">
                                    <span class="detail-label">Valid Until:</span>
                                    <span class="detail-value">%s</span>
                                </div>
                                <div class="detail-row">
                                    <span class="detail-label">Total Amount:</span>
                                    <span class="detail-value amount">%s</span>
                                </div>
                            </div>
                            
                            <p>The detailed quotation is attached as a PDF file. Please review it and let us know if you have any questions.</p>
                            
                            <p>This quotation is valid until <strong>%s</strong>. Please feel free to contact us if you need any clarification.</p>
                            
                            <p>We look forward to doing business with you.</p>
                            
                            <p>Best regards,<br>
                            <strong>%s</strong><br>
                            %s</p>
                        </div>
                        <div class="footer">
                            <p>This is an automated email. Please do not reply to this email.</p>
                            <p>&copy; %s %s. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """,
                quotation.getQuotationNumber(),
                quotation.getCompany().getCompanyName(),
                quotation.getCustomer().getCustomerName(),
                quotation.getQuotationNumber(),
                quotation.getCreatedAt().format(dateFormatter),
                quotation.getExpiryDate().format(dateFormatter),
                currencyFormat.format(quotation.getTotalAmount()),
                quotation.getExpiryDate().format(dateFormatter),
                quotation.getCompany().getCompanyName(),
                getCreatorName(quotation),
                LocalDateTime.now().getYear(),
                quotation.getCompany().getCompanyName()
        );
    }

    private String buildApprovedEmailBody(Quotation quotation) {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }
                        .content { padding: 20px; background-color: #f9f9f9; }
                        .success-icon { font-size: 48px; text-align: center; margin: 20px 0; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Quotation Approved!</h1>
                        </div>
                        <div class="content">
                            <div class="success-icon">✅</div>
                            <p>Dear %s,</p>
                            <p>Great news! Your quotation <strong>%s</strong> has been approved by the customer.</p>
                            <p><strong>Customer:</strong> %s</p>
                            <p><strong>Amount:</strong> %s</p>
                            <p>Please proceed with the next steps as per your company's process.</p>
                            <p>Best regards,<br>Quotation System</p>
                        </div>
                    </div>
                </body>
                </html>
                """,
                getCreatorName(quotation),
                quotation.getQuotationNumber(),
                quotation.getCustomer().getCustomerName(),
                currencyFormat.format(quotation.getTotalAmount())
        );
    }

    private String buildRejectedEmailBody(Quotation quotation) {
        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background-color: #f44336; color: white; padding: 20px; text-align: center; }
                        .content { padding: 20px; background-color: #f9f9f9; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Quotation Rejected</h1>
                        </div>
                        <div class="content">
                            <p>Dear %s,</p>
                            <p>Your quotation <strong>%s</strong> has been rejected by the customer.</p>
                            <p><strong>Customer:</strong> %s</p>
                            <p>You may want to follow up with the customer to understand their concerns or create a revised quotation.</p>
                            <p>Best regards,<br>Quotation System</p>
                        </div>
                    </div>
                </body>
                </html>
                """,
                getCreatorName(quotation),
                quotation.getQuotationNumber(),
                quotation.getCustomer().getCustomerName()
        );
    }

    private String buildExpiryWarningEmailBody(Quotation quotation) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");

        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background-color: #FF9800; color: white; padding: 20px; text-align: center; }
                        .content { padding: 20px; background-color: #f9f9f9; }
                        .warning { background-color: #fff3cd; border-left: 4px solid #FF9800; padding: 15px; margin: 15px 0; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>⚠️ Quotation Expiring Soon</h1>
                        </div>
                        <div class="content">
                            <p>Dear %s,</p>
                            <div class="warning">
                                <p><strong>Reminder:</strong> Your quotation <strong>%s</strong> will expire on <strong>%s</strong>.</p>
                            </div>
                            <p><strong>Customer:</strong> %s</p>
                            <p><strong>Status:</strong> %s</p>
                            <p>Please follow up with the customer if you haven't received a response yet.</p>
                            <p>Best regards,<br>Quotation System</p>
                        </div>
                    </div>
                </body>
                </html>
                """,
                getCreatorName(quotation),
                quotation.getQuotationNumber(),
                quotation.getExpiryDate().format(dateFormatter),
                quotation.getCustomer().getCustomerName(),
                quotation.getStatus()
        );
    }

    private String buildUpdatedEmailBody(Quotation quotation) {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background-color: #2196F3; color: white; padding: 20px; text-align: center; }
                        .content { padding: 20px; background-color: #f9f9f9; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Quotation Updated</h1>
                        </div>
                        <div class="content">
                            <p>Dear %s,</p>
                            <p>Your quotation <strong>%s</strong> has been updated.</p>
                            <p><strong>New Total Amount:</strong> %s</p>
                            <p>Please review the updated quotation attached to this email.</p>
                            <p>Best regards,<br>%s</p>
                        </div>
                    </div>
                </body>
                </html>
                """,
                quotation.getCustomer().getCustomerName(),
                quotation.getQuotationNumber(),
                currencyFormat.format(quotation.getTotalAmount()),
                quotation.getCompany().getCompanyName()
        );
    }
}
