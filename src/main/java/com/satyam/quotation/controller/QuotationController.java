package com.satyam.quotation.controller;

import java.io.File;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.satyam.quotation.dto.QuotationDTO;
import com.satyam.quotation.dto.QuotationRequestDTO;
import com.satyam.quotation.dto.InvoiceRequestDTO;
import com.satyam.quotation.mapper.QuotationMapper;
import com.satyam.quotation.model.Company;
import com.satyam.quotation.model.EmailLog;
import com.satyam.quotation.model.Quotation;
import com.satyam.quotation.security.CustomUserDetails;
import com.satyam.quotation.service.AppSettingsService;
import com.satyam.quotation.service.EmailService;
import com.satyam.quotation.service.InvoiceService;
import com.satyam.quotation.service.QuotationService;
import com.satyam.quotation.service.WhatsAppService;

@RestController
@RequestMapping("/api/quotations")
public class QuotationController {

    private static final Logger log = LoggerFactory.getLogger(QuotationController.class);

    private final QuotationService quotationService;
    private final QuotationMapper quotationMapper;
    private final EmailService emailService;
    private final WhatsAppService whatsAppService;
    private final AppSettingsService appSettingsService;
    private final InvoiceService invoiceService;

    public QuotationController(QuotationService quotationService,
                              QuotationMapper quotationMapper,
                              EmailService emailService,
                              WhatsAppService whatsAppService,
                              AppSettingsService appSettingsService,
                              InvoiceService invoiceService) {
        this.quotationService = quotationService;
        this.quotationMapper = quotationMapper;
        this.emailService = emailService;
        this.whatsAppService = whatsAppService;
        this.appSettingsService = appSettingsService;
        this.invoiceService = invoiceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuotationDTO createQuotation(@RequestBody @Valid QuotationRequestDTO requestDTO,
                                       Authentication authentication) {

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();

        log.info("User {} creating quotation for customer {}", user.getUserId(), requestDTO.getCustomerId());

        // Convert DTO to entity using mapper
        Quotation quotation = quotationMapper.toEntity(requestDTO);

        // Set user and company
        quotation.setCreatedBy(user.getUserId());
        if (user.getCompanyId() != null) {
            Company company = new Company();
            company.setId(user.getCompanyId());
            quotation.setCompany(company);
        }

        // Map services from DTO
        if (requestDTO.getServices() != null && !requestDTO.getServices().isEmpty()) {
            List<com.satyam.quotation.model.QuotationService> services = requestDTO.getServices().stream()
                .map(s -> com.satyam.quotation.model.QuotationService.builder()
                    .serviceName(s.getServiceName())
                    .servicePrice(s.getServicePrice() != null ? s.getServicePrice() : java.math.BigDecimal.ZERO)
                    .serviceTax(s.getServiceTax() != null ? s.getServiceTax() : java.math.BigDecimal.ZERO)
                    .quotation(quotation)
                    .build())
                .collect(java.util.stream.Collectors.toList());
            quotation.setServices(services);
        }

        Quotation saved = quotationService.createQuotation(quotation);
        return quotationMapper.toDto(saved);
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<QuotationDTO> getQuotations(Authentication authentication) {

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();

        log.info("Fetching quotations for user {} (role: {}, companyId: {})", 
                user.getUserId(), user.getRole(), user.getCompanyId());

        List<Quotation> quotations;

        if ("SUPER_ADMIN".equals(user.getRole())) {
            log.info("User is SUPER_ADMIN, fetching all quotations");
            quotations = quotationService.getAllQuotations();
        } else if ("CLIENT".equals(user.getRole())) {
            log.info("User is CLIENT, fetching quotations for company {}", user.getCompanyId());
            quotations = quotationService.getQuotationsByCompany(user.getCompanyId());
        } else {
            log.info("User is {}, fetching quotations for user {}", user.getRole(), user.getUserId());
            quotations = quotationService.getQuotationsByUser(user.getUserId());
        }

        log.info("Found {} quotations", quotations.size());

        return quotations.stream()
                .map(quotationMapper::toDto)
                .toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public QuotationDTO getQuotation(@PathVariable Long id) {
        return quotationService.getQuotationById(id)
                .map(quotationMapper::toDto)
                .orElseThrow(() -> new com.satyam.quotation.exception.ResourceNotFoundException(
                        "Quotation not found with id: " + id));
    }

    @PutMapping("/{id}")
    public QuotationDTO updateQuotation(
            @PathVariable Long id,
            @RequestBody Quotation quotation,
            Authentication authentication) {

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();

        log.info("User {} updating quotation {}", user.getUserId(), id);

        // Get old status before update
        Quotation existingQuotation = quotationService.getQuotationById(id)
                .orElseThrow(() -> new com.satyam.quotation.exception.ResourceNotFoundException(
                        "Quotation not found"));

        String oldStatus = existingQuotation.getStatus();
        String newStatus = quotation.getStatus();

        Quotation updated = quotationService.updateQuotation(id, quotation, user.getUserId());

        // Send email notifications based on status change
        if (!oldStatus.equals(newStatus)) {
            handleStatusChangeNotification(updated, oldStatus, newStatus);
        }

        return quotationMapper.toDto(updated);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQuotation(
            @PathVariable Long id,
            Authentication authentication) {

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();

        log.info("User {} deleting quotation {}", user.getUserId(), id);

        quotationService.deleteQuotation(id, user.getUserId());
    }

    /**
     * Send quotation email to customer
     */
    @PostMapping("/{id}/send-email")
    public ResponseEntity<?> sendQuotationEmail(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> request,
            Authentication authentication) {

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();

        log.info("User {} sending quotation email for quotation {}", user.getUserId(), id);

        try {
            Quotation quotation = quotationService.getQuotationById(id)
                    .orElseThrow(() -> new com.satyam.quotation.exception.ResourceNotFoundException(
                            "Quotation not found"));

            // Get PDF file path (assuming it's already generated)
            File pdfFile = null;
            if (quotation.getPdfPath() != null && !quotation.getPdfPath().isEmpty()) {
                pdfFile = new File(quotation.getPdfPath());
                if (!pdfFile.exists()) {
                    log.warn("PDF file not found at: {}", quotation.getPdfPath());
                    pdfFile = null;
                }
            }

            // Send email
            EmailLog emailLog = emailService.sendQuotationEmail(quotation, pdfFile, user.getUser());

            // Update quotation status to SENT if email was successful
            if (emailLog.getStatus() == EmailLog.EmailStatus.SUCCESS) {
                quotationService.changeStatus(id, "SENT", user.getUserId());
            }

            return ResponseEntity.ok(Map.of(
                    "success", emailLog.getStatus() == EmailLog.EmailStatus.SUCCESS,
                    "message", emailLog.getStatus() == EmailLog.EmailStatus.SUCCESS
                            ? "Email sent successfully"
                            : "Email failed: " + emailLog.getErrorMessage(),
                    "emailLogId", emailLog.getId()
            ));

        } catch (Exception e) {
            log.error("Failed to send quotation email", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Failed to send email: " + e.getMessage()
                    ));
        }
    }

    /**
     * Retry failed email
     */
    @PostMapping("/email/{emailLogId}/retry")
    public ResponseEntity<?> retryEmail(
            @PathVariable Long emailLogId,
            Authentication authentication) {

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();

        log.info("User {} retrying email {}", user.getUserId(), emailLogId);

        try {
            EmailLog emailLog = emailService.retryFailedEmail(emailLogId);

            return ResponseEntity.ok(Map.of(
                    "success", emailLog.getStatus() == EmailLog.EmailStatus.SUCCESS,
                    "message", emailLog.getStatus() == EmailLog.EmailStatus.SUCCESS
                            ? "Email sent successfully"
                            : "Email failed: " + emailLog.getErrorMessage()
            ));

        } catch (Exception e) {
            log.error("Failed to retry email", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Failed to retry email: " + e.getMessage()
                    ));
        }
    }

    /**
     * Duplicate quotation (for revising rejected quotations)
     */
    @PostMapping("/{id}/duplicate")
    public QuotationDTO duplicateQuotation(
            @PathVariable Long id,
            Authentication authentication) {

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();

        log.info("User {} duplicating quotation {}", user.getUserId(), id);

        Quotation duplicated = quotationService.duplicateQuotation(id, user.getUserId());
        return quotationMapper.toDto(duplicated);
    }

    /**
     * Get notification settings for frontend to determine valid status transitions
     */
    @GetMapping("/notification-settings")
    public ResponseEntity<?> getNotificationSettings() {
        boolean emailEnabled = appSettingsService.getBooleanSetting("email_notifications_enabled", true);
        boolean whatsappEnabled = appSettingsService.getBooleanSetting("whatsapp_notifications_enabled", false);
        boolean notificationsEnabled = emailEnabled || whatsappEnabled;
        return ResponseEntity.ok(Map.of(
                "emailEnabled", emailEnabled,
                "whatsappEnabled", whatsappEnabled,
                "notificationsEnabled", notificationsEnabled
        ));
    }

    /**
     * Change quotation status
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> changeStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();

        String newStatus = request.get("status");
        if (newStatus == null || newStatus.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Status is required"));
        }

        log.info("User {} changing quotation {} status to {}", user.getUserId(), id, newStatus);

        try {
            // Get old status before changing
            Quotation existingQuotation = quotationService.getQuotationById(id)
                    .orElseThrow(() -> new com.satyam.quotation.exception.ResourceNotFoundException(
                            "Quotation not found"));
            String oldStatus = existingQuotation.getStatus();

            // Change status
            Quotation quotation = quotationService.changeStatus(id, newStatus, user.getUserId());

            // Send email notifications based on status change
            handleStatusChangeNotification(quotation, oldStatus, newStatus, user.getUserId());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Status updated to " + newStatus,
                    "quotation", quotationMapper.toDto(quotation)
            ));

        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error changing quotation status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to change status: " + e.getMessage()));
        }
    }

    /**
     * Get quotations by status
     */
    @GetMapping("/status/{status}")
    @Transactional(readOnly = true)
    public List<QuotationDTO> getQuotationsByStatus(
            @PathVariable String status,
            Authentication authentication) {

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();

        log.info("Fetching quotations with status {} for user {}", status, user.getUserId());

        Long companyId = "SUPER_ADMIN".equals(user.getRole()) ? null : user.getCompanyId();

        List<Quotation> quotations = quotationService.getQuotationsByStatus(status, companyId);

        return quotations.stream()
                .map(quotationMapper::toDto)
                .toList();
    }

    /**
     * Get expired quotations
     */
    @GetMapping("/expired")
    @Transactional(readOnly = true)
    public List<QuotationDTO> getExpiredQuotations(Authentication authentication) {

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();

        log.info("Fetching expired quotations for user {}", user.getUserId());

        Long companyId = "SUPER_ADMIN".equals(user.getRole()) ? null : user.getCompanyId();

        List<Quotation> quotations = quotationService.getExpiredQuotations(companyId);

        return quotations.stream()
                .map(quotationMapper::toDto)
                .toList();
    }

    /**
     * Handle status change notifications
     */
    private void handleStatusChangeNotification(Quotation quotation, String oldStatus, String newStatus) {
        handleStatusChangeNotification(quotation, oldStatus, newStatus, null);
    }

    private void handleStatusChangeNotification(Quotation quotation, String oldStatus, String newStatus, Long userId) {
        try {
            switch (newStatus) {
                case "APPROVED":
                    emailService.sendQuotationApprovedEmail(quotation);
                    log.info("Sent approval notification for quotation: {}", quotation.getQuotationNumber());
                    break;

                case "REJECTED":
                    emailService.sendQuotationRejectedEmail(quotation);
                    log.info("Sent rejection notification for quotation: {}", quotation.getQuotationNumber());
                    break;

                default:
                    break;
            }
        } catch (Exception e) {
            log.error("Failed to send email notification for quotation: {}", quotation.getQuotationNumber(), e);
        }

        try {
            whatsAppService.sendStatusChangeNotification(quotation, oldStatus, newStatus);
        } catch (Exception e) {
            log.error("Failed to send WhatsApp notification for quotation: {}", quotation.getQuotationNumber(), e);
        }

        // Auto-create invoice when quotation is approved
        if ("APPROVED".equals(newStatus)) {
            try {
                Long invoiceCreatorId = userId != null ? userId : quotation.getCreatedBy();
                InvoiceRequestDTO invoiceRequest = new InvoiceRequestDTO();
                invoiceRequest.setQuotationId(quotation.getId());
                invoiceRequest.setInvoiceDate(java.time.LocalDate.now());
                invoiceRequest.setDueDate(java.time.LocalDate.now().plusDays(30));
                invoiceService.createInvoice(invoiceRequest, invoiceCreatorId);
                log.info("Auto-created invoice for approved quotation: {}", quotation.getQuotationNumber());
            } catch (Exception e) {
                log.error("Failed to auto-create invoice for quotation: {}", quotation.getQuotationNumber(), e);
            }
        }
    }
}
