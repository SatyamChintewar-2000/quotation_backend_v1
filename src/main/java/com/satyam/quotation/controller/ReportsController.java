package com.satyam.quotation.controller;

import com.satyam.quotation.dto.StaffSummaryDTO;
import com.satyam.quotation.exception.UnauthorizedException;
import com.satyam.quotation.repository.CustomerRepository;
import com.satyam.quotation.repository.EnquiryRepository;
import com.satyam.quotation.repository.InvoiceRepository;
import com.satyam.quotation.repository.QuotationRepository;
import com.satyam.quotation.repository.UserRepository;
import com.satyam.quotation.security.CustomUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportsController {

    private static final Logger log = LoggerFactory.getLogger(ReportsController.class);

    private final UserRepository userRepository;
    private final EnquiryRepository enquiryRepository;
    private final CustomerRepository customerRepository;
    private final QuotationRepository quotationRepository;
    private final InvoiceRepository invoiceRepository;

    public ReportsController(UserRepository userRepository,
                              EnquiryRepository enquiryRepository,
                              CustomerRepository customerRepository,
                              QuotationRepository quotationRepository,
                              InvoiceRepository invoiceRepository) {
        this.userRepository = userRepository;
        this.enquiryRepository = enquiryRepository;
        this.customerRepository = customerRepository;
        this.quotationRepository = quotationRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @GetMapping("/staff-summary")
    @Transactional(readOnly = true)
    public List<StaffSummaryDTO> getStaffSummary(Authentication authentication) {
        try {
            log.info("=== STAFF SUMMARY REQUEST START ===");
            CustomUserDetails currentUser = (CustomUserDetails) authentication.getPrincipal();
            String role = currentUser.getRole();
            log.info("User: {}, Role: {}, CompanyId: {}", currentUser.getUsername(), role, currentUser.getCompanyId());

            // Only admin and superadmin can access this
            boolean isAdmin = "ADMIN".equalsIgnoreCase(role)
                    || "CLIENT".equalsIgnoreCase(role)
                    || "SUPERADMIN".equalsIgnoreCase(role)
                    || "SUPER_ADMIN".equalsIgnoreCase(role);

            if (!isAdmin) {
                log.warn("Access denied for role: {}", role);
                throw new UnauthorizedException("Access denied");
            }

            Long companyId = currentUser.getCompanyId();
            boolean isSuperAdmin = "SUPERADMIN".equalsIgnoreCase(role) || "SUPER_ADMIN".equalsIgnoreCase(role);
            log.info("Is SuperAdmin: {}", isSuperAdmin);

            // Get staff users - SUPER_ADMIN sees all users, others see only their company
            List<com.satyam.quotation.model.User> staffUsers;
            if (isSuperAdmin) {
                // SUPER_ADMIN sees all users from all companies
                log.info("Fetching all users for SUPER_ADMIN");
                staffUsers = userRepository.findAll()
                        .stream()
                        .filter(u -> Boolean.TRUE.equals(u.getActive()) && u.getDeletedAt() == null)
                        .toList();
            } else {
                // CLIENT/ADMIN see only their company users
                if (companyId == null) {
                    log.error("CompanyId is null for non-SUPER_ADMIN user");
                    throw new RuntimeException("User does not have a company assigned");
                }
                log.info("Fetching users for company: {}", companyId);
                staffUsers = userRepository.findByCompanyId(companyId)
                        .stream()
                        .filter(u -> Boolean.TRUE.equals(u.getActive()) && u.getDeletedAt() == null)
                        .toList();
            }

            log.info("Found {} staff users", staffUsers.size());

            List<StaffSummaryDTO> result = staffUsers.stream().map(u -> {
                try {
                    log.debug("Processing user: {} (ID: {})", u.getName(), u.getId());
                    
                    // Count enquiries
                    long enquiries = 0;
                    try {
                        List<com.satyam.quotation.model.Enquiry> enquiryList = enquiryRepository.findByCreatedByAndDeletedAtIsNull(u.getId());
                        enquiries = enquiryList != null ? enquiryList.size() : 0;
                    } catch (Exception e) {
                        log.error("Error counting enquiries for user {}: {}", u.getId(), e.getMessage());
                    }

                    // Count customers
                    long customers = 0;
                    try {
                        List<com.satyam.quotation.model.Customer> customerList = customerRepository.findByCreatedBy(u.getId());
                        if (customerList != null) {
                            customers = customerList.stream()
                                    .filter(c -> Boolean.TRUE.equals(c.getActive()))
                                    .count();
                        }
                    } catch (Exception e) {
                        log.error("Error counting customers for user {}: {}", u.getId(), e.getMessage());
                    }

                    // Count quotations
                    long quotations = 0;
                    try {
                        List<com.satyam.quotation.model.Quotation> quotationList = quotationRepository.findByCreatedBy(u.getId());
                        quotations = quotationList != null ? quotationList.size() : 0;
                    } catch (Exception e) {
                        log.error("Error counting quotations for user {}: {}", u.getId(), e.getMessage());
                    }

                    // Count invoices - use simple approach
                    long invoices = 0;
                    try {
                        invoices = invoiceRepository.countByCreatedByAndActiveTrue(u.getId());
                    } catch (Exception e) {
                        log.error("Error counting invoices for user {}: {}", u.getId(), e.getMessage());
                        // Fallback: try to count manually if the repository method fails
                        try {
                            List<com.satyam.quotation.model.Invoice> allInvoices = invoiceRepository.findAll();
                            if (allInvoices != null) {
                                invoices = allInvoices.stream()
                                        .filter(i -> u.getId().equals(i.getCreatedBy()) && Boolean.TRUE.equals(i.getActive()))
                                        .count();
                            }
                        } catch (Exception e2) {
                            log.error("Fallback invoice counting also failed for user {}: {}", u.getId(), e2.getMessage());
                        }
                    }

                    // Calculate revenue
                    double revenue = 0.0;
                    try {
                        Double revenueValue = quotationRepository.getTotalRevenueByUser(u.getId());
                        revenue = revenueValue != null ? revenueValue : 0.0;
                    } catch (Exception e) {
                        log.error("Error calculating revenue for user {}: {}", u.getId(), e.getMessage());
                    }

                    // Safely get role name
                    String roleName = "staff";
                    try {
                        if (u.getRole() != null && u.getRole().getRoleName() != null) {
                            roleName = u.getRole().getRoleName();
                        }
                    } catch (Exception e) {
                        log.error("Error getting role for user {}: {}", u.getId(), e.getMessage());
                    }

                    log.debug("User {} stats: enquiries={}, customers={}, quotations={}, invoices={}, revenue={}", 
                            u.getId(), enquiries, customers, quotations, invoices, revenue);

                    return new StaffSummaryDTO(
                            u.getId(),
                            u.getName() != null ? u.getName() : "Unknown",
                            u.getEmail() != null ? u.getEmail() : "",
                            roleName,
                            enquiries,
                            customers,
                            quotations,
                            invoices,
                            revenue
                    );
                } catch (Exception e) {
                    // Log error but continue with other users
                    log.error("Error processing user {}: {}", u.getId(), e.getMessage(), e);
                    return new StaffSummaryDTO(
                            u.getId(),
                            u.getName() != null ? u.getName() : "Unknown",
                            u.getEmail() != null ? u.getEmail() : "",
                            "staff",
                            0, 0, 0, 0, 0.0
                    );
                }
            }).toList();

            log.info("=== STAFF SUMMARY REQUEST END - Returning {} records ===", result.size());
            return result;
        } catch (Exception e) {
            log.error("=== STAFF SUMMARY REQUEST FAILED ===", e);
            throw e;
        }
    }
}
