package com.satyam.quotation.controller;

import java.io.File;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.satyam.quotation.dto.QuotationDTO;
import com.satyam.quotation.dto.QuotationListDTO;
import com.satyam.quotation.dto.QuotationRequestDTO;
import com.satyam.quotation.mapper.QuotationMapper;
import com.satyam.quotation.model.Company;
import com.satyam.quotation.model.EmailLog;
import com.satyam.quotation.model.Quotation;
import com.satyam.quotation.security.CustomUserDetails;
import com.satyam.quotation.service.AppSettingsService;
import com.satyam.quotation.service.EmailService;
import com.satyam.quotation.service.impl.PostStatusChangeService;
import com.satyam.quotation.service.QuotationService;

@RestController
@RequestMapping("/api/quotations")
public class QuotationController {

    private static final Logger log = LoggerFactory.getLogger(QuotationController.class);

    private final QuotationService quotationService;
    private final QuotationMapper quotationMapper;
    private final EmailService emailService;
    private final AppSettingsService appSettingsService;
    private final PostStatusChangeService postStatusChangeService;

    public QuotationController(QuotationService quotationService,
                              QuotationMapper quotationMapper,
                              EmailService emailService,
                              AppSettingsService appSettingsService,
                              PostStatusChangeService postStatusChangeService) {
        this.quotationService = quotationService;
        this.quotationMapper = quotationMapper;
        this.emailService = emailService;
        this.appSettingsService = appSettingsService;
        this.postStatusChangeService = postStatusChangeService;
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

        // Capture address snapshots — use direct save to avoid the edit-status guard
        if (saved.getCustomer() != null) {
            String custAddr = requestDTO.getCustomerAddress() != null
                    ? requestDTO.getCustomerAddress()
                    : saved.getCustomer().getAddress();
            String shipAddr = requestDTO.getShippingAddress() != null
                    ? requestDTO.getShippingAddress()
                    : saved.getCustomer().getShippingAddress();
            if (custAddr != null || shipAddr != null) {
                quotationService.saveAddressSnapshots(saved.getId(), custAddr, shipAddr);
                // Reload so the returned DTO has the addresses
                saved = quotationService.getQuotationById(saved.getId()).orElse(saved);
            }
        }

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
            quotations = quotationService.getAllQuotations();
        } else if ("CLIENT".equals(user.getRole())) {
            quotations = quotationService.getQuotationsByCompany(user.getCompanyId());
        } else {
            quotations = quotationService.getQuotationsByUser(user.getUserId());
        }

        log.info("Found {} quotations", quotations.size());

        return quotations.stream()
                .map(quotationMapper::toDto)
                .toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public QuotationDTO getQuotation(@PathVariable("id") Long id) {
        return quotationService.getQuotationById(id)
                .map(quotationMapper::toDto)
                .orElseThrow(() -> new com.satyam.quotation.exception.ResourceNotFoundException(
                        "Quotation not found with id: " + id));
    }

    /**
     * Paginated list endpoint — returns QuotationListDTO (NO product images).
     *
     * Query params:
     *   page  (default 0)   — zero-based page number
     *   size  (default 10)  — records per page
     *   sort  (default createdAt,desc)
     *
     * Example: GET /api/quotations/paged?page=0&size=10&sort=createdAt,desc
     */
    @GetMapping("/paged")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getQuotationsPaged(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();

        // Always sort by newest first
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Quotation> quotationPage;
        if ("SUPER_ADMIN".equals(user.getRole())) {
            quotationPage = quotationService.getAllQuotationsPaged(pageable);
        } else if ("CLIENT".equals(user.getRole())) {
            quotationPage = quotationService.getQuotationsByCompanyPaged(user.getCompanyId(), pageable);
        } else {
            quotationPage = quotationService.getQuotationsByUserPaged(user.getUserId(), pageable);
        }

        // Map to lightweight DTO — NO images in items
        List<QuotationListDTO> content = quotationPage.getContent().stream()
                .map(q -> toListDto(q))
                .toList();

        return ResponseEntity.ok(Map.of(
                "content",       content,
                "totalElements", quotationPage.getTotalElements(),
                "totalPages",    quotationPage.getTotalPages(),
                "currentPage",   quotationPage.getNumber(),
                "pageSize",      quotationPage.getSize()
        ));
    }

    /** Map Quotation entity → QuotationListDTO (no images). */
    private QuotationListDTO toListDto(Quotation q) {
        QuotationListDTO dto = new QuotationListDTO();
        dto.setId(q.getId());
        dto.setQuotationNumber(q.getQuotationNumber());
        dto.setCustomerId(q.getCustomer() != null ? q.getCustomer().getId() : null);
        dto.setCustomerName(q.getCustomer() != null ? q.getCustomer().getCustomerName() : null);
        dto.setCustomerPhone(q.getCustomer() != null ? q.getCustomer().getPhone() : null);
        dto.setSubtotal(q.getSubtotal());
        dto.setTotalDiscount(q.getTotalDiscount());
        dto.setTotalGst(q.getTotalGst());
        dto.setTotalAmount(q.getTotalAmount());
        dto.setStatus(q.getStatus());
        dto.setExpiryDate(q.getExpiryDate());
        dto.setQuotationDate(q.getQuotationDate());
        dto.setQuotationCode(q.getQuotationCode());
        dto.setDeliveryDate(q.getDeliveryDate());
        dto.setExecutiveName(q.getExecutiveName());
        dto.setNotes(q.getNotes());
        dto.setCreatedAt(q.getCreatedAt());
        dto.setCreatedBy(q.getCreatedBy());
        dto.setHideServiceChargesOnPdf(q.getHideServiceChargesOnPdf());
        dto.setCustomerAddress(q.getCustomerAddress());
        dto.setShippingAddress(q.getShippingAddress());

        // Map items WITHOUT images
        if (q.getItems() != null) {
            dto.setItems(q.getItems().stream().map(item -> {
                QuotationListDTO.QuotationListItemDTO i = new QuotationListDTO.QuotationListItemDTO();
                i.setId(item.getId());
                i.setProductId(item.getProduct() != null ? item.getProduct().getId() : null);
                // Use snapshot name first (captured at time of quoting)
                i.setProductName(item.getProductNameSnapshot() != null
                        ? item.getProductNameSnapshot()
                        : (item.getProductName() != null ? item.getProductName()
                        : (item.getProduct() != null ? item.getProduct().getProductName() : null)));
                i.setProductDescription(item.getProductDescriptionSnapshot() != null
                        ? item.getProductDescriptionSnapshot()
                        : item.getProductDescription());
                i.setUnitPrice(item.getUnitPrice());
                i.setQuantity(item.getQuantity());
                i.setDiscountPercentage(item.getDiscountPercentage());
                i.setTaxPercentage(item.getTaxPercentage());
                i.setItemTotal(item.getItemTotal());
                // imagePathSnapshot intentionally excluded
                return i;
            }).toList());
        }

        return dto;
    }

    @PutMapping("/{id}")
    public QuotationDTO updateQuotation(
            @PathVariable("id") Long id,
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
            @PathVariable("id") Long id,
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
            @PathVariable("id") Long id,
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
            @PathVariable("emailLogId") Long emailLogId,
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
            @PathVariable("id") Long id,
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
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> changeStatus(
            @PathVariable("id") Long id,
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
            @PathVariable("status") String status,
            Authentication authentication) {

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();

        log.info("Fetching quotations with status {} for user {}", status, user.getUserId());

        Long companyId = user.getCompanyId();

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

        Long companyId = user.getCompanyId();

        List<Quotation> quotations = quotationService.getExpiredQuotations(companyId);

        return quotations.stream()
                .map(quotationMapper::toDto)
                .toList();
    }

    /**
     * Handle status change notifications — delegates to a REQUIRES_NEW transaction
     * so failures never roll back the status change itself.
     */
    private void handleStatusChangeNotification(Quotation quotation, String oldStatus, String newStatus) {
        handleStatusChangeNotification(quotation, oldStatus, newStatus, null);
    }

    private void handleStatusChangeNotification(Quotation quotation, String oldStatus, String newStatus, Long userId) {
        try {
            postStatusChangeService.handlePostStatusChange(quotation, oldStatus, newStatus, userId);
        } catch (Exception e) {
            log.error("Post-status-change processing failed for quotation {}: {}",
                    quotation.getQuotationNumber(), e.getMessage());
        }
    }
}
