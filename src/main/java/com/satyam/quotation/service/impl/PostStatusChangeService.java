package com.satyam.quotation.service.impl;

import com.satyam.quotation.dto.InvoiceRequestDTO;
import com.satyam.quotation.model.Quotation;
import com.satyam.quotation.service.EmailService;
import com.satyam.quotation.service.InvoiceService;
import com.satyam.quotation.service.WhatsAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class PostStatusChangeService {

    private static final Logger log = LoggerFactory.getLogger(PostStatusChangeService.class);

    private final EmailService emailService;
    private final WhatsAppService whatsAppService;
    private final InvoiceService invoiceService;

    public PostStatusChangeService(EmailService emailService,
                                   WhatsAppService whatsAppService,
                                   InvoiceService invoiceService) {
        this.emailService = emailService;
        this.whatsAppService = whatsAppService;
        this.invoiceService = invoiceService;
    }

    /**
     * Runs in a NEW transaction so failures here never roll back the status change.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePostStatusChange(Quotation quotation, String oldStatus, String newStatus, Long userId) {

        // Email notifications
        try {
            switch (newStatus) {
                case "APPROVED" -> emailService.sendQuotationApprovedEmail(quotation);
                case "REJECTED" -> emailService.sendQuotationRejectedEmail(quotation);
            }
        } catch (Exception e) {
            log.error("Failed to send email for quotation {}: {}", quotation.getQuotationNumber(), e.getMessage());
        }

        // WhatsApp notifications
        try {
            whatsAppService.sendStatusChangeNotification(quotation, oldStatus, newStatus);
        } catch (Exception e) {
            log.error("Failed to send WhatsApp for quotation {}: {}", quotation.getQuotationNumber(), e.getMessage());
        }

        // Auto-create invoice on APPROVED
        if ("APPROVED".equals(newStatus)) {
            try {
                Long creatorId = userId != null ? userId : quotation.getCreatedBy();
                InvoiceRequestDTO req = new InvoiceRequestDTO();
                req.setQuotationId(quotation.getId());
                req.setInvoiceDate(LocalDate.now());
                req.setDueDate(LocalDate.now().plusDays(30));
                invoiceService.createInvoice(req, creatorId);
                log.info("Auto-created invoice for quotation: {}", quotation.getQuotationNumber());
            } catch (Exception e) {
                log.error("Failed to auto-create invoice for quotation {}: {}", quotation.getQuotationNumber(), e.getMessage());
            }
        }
    }
}
