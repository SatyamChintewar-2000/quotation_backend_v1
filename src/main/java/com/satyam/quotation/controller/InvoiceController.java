package com.satyam.quotation.controller;

import com.satyam.quotation.dto.InvoiceDTO;
import com.satyam.quotation.dto.InvoiceRequestDTO;
import com.satyam.quotation.dto.PaymentDTO;
import com.satyam.quotation.security.CustomUserDetails;
import com.satyam.quotation.service.InvoiceService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private static final Logger log = LoggerFactory.getLogger(InvoiceController.class);

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceDTO createInvoice(@RequestBody @Valid InvoiceRequestDTO requestDTO,
                                    Authentication authentication) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        log.info("User {} creating invoice from quotation {}", user.getUserId(), requestDTO.getQuotationId());
        return invoiceService.createInvoice(requestDTO, user.getUserId());
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<InvoiceDTO> getInvoices(Authentication authentication) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        log.info("Fetching invoices for company {}", user.getCompanyId());
        return invoiceService.getInvoicesByCompany(user.getCompanyId());
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public InvoiceDTO getInvoice(@PathVariable Long id) {
        log.info("Fetching invoice {}", id);
        return invoiceService.getInvoiceById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
    }

    @GetMapping("/number/{invoiceNumber}")
    @Transactional(readOnly = true)
    public InvoiceDTO getInvoiceByNumber(@PathVariable String invoiceNumber) {
        log.info("Fetching invoice by number: {}", invoiceNumber);
        return invoiceService.getInvoiceByNumber(invoiceNumber);
    }

    @GetMapping("/quotation/{quotationId}")
    @Transactional(readOnly = true)
    public List<InvoiceDTO> getInvoicesByQuotation(@PathVariable Long quotationId) {
        log.info("Fetching invoices for quotation {}", quotationId);
        return invoiceService.getInvoicesByQuotation(quotationId);
    }

    @GetMapping("/customer/{customerId}")
    @Transactional(readOnly = true)
    public List<InvoiceDTO> getInvoicesByCustomer(@PathVariable Long customerId) {
        log.info("Fetching invoices for customer {}", customerId);
        return invoiceService.getInvoicesByCustomer(customerId);
    }

    @GetMapping("/status/{status}")
    @Transactional(readOnly = true)
    public List<InvoiceDTO> getInvoicesByStatus(@PathVariable String status,
                                                Authentication authentication) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        log.info("Fetching invoices with status {} for company {}", status, user.getCompanyId());
        return invoiceService.getInvoicesByStatus(user.getCompanyId(), status);
    }

    @GetMapping("/payment-status/{paymentStatus}")
    @Transactional(readOnly = true)
    public List<InvoiceDTO> getInvoicesByPaymentStatus(@PathVariable String paymentStatus,
                                                       Authentication authentication) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        log.info("Fetching invoices with payment status {} for company {}", paymentStatus, user.getCompanyId());
        return invoiceService.getInvoicesByPaymentStatus(user.getCompanyId(), paymentStatus);
    }

    @GetMapping("/overdue")
    @Transactional(readOnly = true)
    public List<InvoiceDTO> getOverdueInvoices(Authentication authentication) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        log.info("Fetching overdue invoices for company {}", user.getCompanyId());
        return invoiceService.getOverdueInvoices(user.getCompanyId());
    }

    @GetMapping("/date-range")
    @Transactional(readOnly = true)
    public List<InvoiceDTO> getInvoicesByDateRange(@RequestParam LocalDate startDate,
                                                   @RequestParam LocalDate endDate,
                                                   Authentication authentication) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        log.info("Fetching invoices between {} and {} for company {}", startDate, endDate, user.getCompanyId());
        return invoiceService.getInvoicesByDateRange(user.getCompanyId(), startDate, endDate);
    }

    @PutMapping("/{id}")
    public InvoiceDTO updateInvoice(@PathVariable Long id,
                                    @RequestBody @Valid InvoiceRequestDTO requestDTO,
                                    Authentication authentication) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        log.info("User {} updating invoice {}", user.getUserId(), id);
        return invoiceService.updateInvoice(id, requestDTO, user.getUserId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteInvoice(@PathVariable Long id,
                              Authentication authentication) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        log.info("User {} deleting invoice {}", user.getUserId(), id);
        invoiceService.deleteInvoice(id, user.getUserId());
    }

    @PutMapping("/{id}/status")
    public InvoiceDTO changeStatus(@PathVariable Long id,
                                   @RequestBody Map<String, String> request,
                                   Authentication authentication) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        String newStatus = request.get("status");
        log.info("User {} changing invoice {} status to {}", user.getUserId(), id, newStatus);
        return invoiceService.changeStatus(id, newStatus, user.getUserId());
    }

    @PutMapping("/{id}/mark-as-sent")
    public InvoiceDTO markAsSent(@PathVariable Long id,
                                 Authentication authentication) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        log.info("User {} marking invoice {} as sent", user.getUserId(), id);
        return invoiceService.markAsSent(id, user.getUserId());
    }

    @PutMapping("/{id}/mark-as-paid")
    public InvoiceDTO markAsPaid(@PathVariable Long id,
                                 Authentication authentication) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        log.info("User {} marking invoice {} as paid", user.getUserId(), id);
        return invoiceService.markAsPaid(id, user.getUserId());
    }

    @PostMapping("/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentDTO recordPayment(@PathVariable Long id,
                                    @RequestBody @Valid PaymentDTO paymentDTO,
                                    Authentication authentication) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        log.info("User {} recording payment for invoice {}", user.getUserId(), id);
        return invoiceService.recordPayment(id, paymentDTO, user.getUserId());
    }

    @GetMapping("/{id}/payments")
    @Transactional(readOnly = true)
    public List<PaymentDTO> getPaymentHistory(@PathVariable Long id) {
        log.info("Fetching payment history for invoice {}", id);
        return invoiceService.getPaymentHistory(id);
    }

    @DeleteMapping("/{invoiceId}/payments/{paymentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePayment(@PathVariable Long invoiceId,
                              @PathVariable Long paymentId,
                              Authentication authentication) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        log.info("User {} deleting payment {} for invoice {}", user.getUserId(), paymentId, invoiceId);
        invoiceService.deletePayment(paymentId, user.getUserId());
    }
}
