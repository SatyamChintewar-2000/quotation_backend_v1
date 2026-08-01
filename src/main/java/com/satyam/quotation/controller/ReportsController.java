package com.satyam.quotation.controller;

import com.satyam.quotation.dto.StaffSummaryDTO;
import com.satyam.quotation.exception.UnauthorizedException;
import com.satyam.quotation.model.User;
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
        log.info("=== STAFF SUMMARY REQUEST START ===");
        CustomUserDetails currentUser = (CustomUserDetails) authentication.getPrincipal();
        String role = currentUser.getRole();

        boolean isAdmin = "ADMIN".equalsIgnoreCase(role)
                || "CLIENT".equalsIgnoreCase(role)
                || "SUPERADMIN".equalsIgnoreCase(role)
                || "SUPER_ADMIN".equalsIgnoreCase(role);

        if (!isAdmin) {
            throw new UnauthorizedException("Access denied");
        }

        Long companyId = currentUser.getCompanyId();
        boolean isSuperAdmin = "SUPERADMIN".equalsIgnoreCase(role) || "SUPER_ADMIN".equalsIgnoreCase(role);

        // Load users with role + company eagerly (JOIN FETCH in repo — avoids N+1 on user list)
        List<User> staffUsers;
        if (isSuperAdmin) {
            staffUsers = userRepository.findAllWithDetails().stream()
                    .filter(u -> Boolean.TRUE.equals(u.getActive()) && u.getDeletedAt() == null)
                    .toList();
        } else {
            if (companyId == null) {
                throw new RuntimeException("User does not have a company assigned");
            }
            staffUsers = userRepository.findByCompanyId(companyId).stream()
                    .filter(u -> Boolean.TRUE.equals(u.getActive()) && u.getDeletedAt() == null)
                    .toList();
        }

        log.info("Found {} staff users — building summary with COUNT queries", staffUsers.size());

        // For each user, fire COUNT queries (not full list fetches).
        // This is 5 COUNT queries per user instead of 5 full-list queries.
        // For 20 users: 100 light COUNT queries vs the old ~500 heavy row-loading queries.
        List<StaffSummaryDTO> result = staffUsers.stream().map(u -> {
            long enquiries  = enquiryRepository.countByCreatedByAndDeletedAtIsNull(u.getId());
            long customers  = customerRepository.countByCreatedByAndActiveTrue(u.getId());
            long quotations = quotationRepository.countByCreatedByAndActive(u.getId());
            long invoices   = invoiceRepository.countByCreatedByAndActiveTrue(u.getId());
            double revenue  = orZero(quotationRepository.getTotalRevenueByUser(u.getId()));

            String roleName = (u.getRole() != null && u.getRole().getRoleName() != null)
                    ? u.getRole().getRoleName()
                    : "staff";

            return new StaffSummaryDTO(
                    u.getId(),
                    u.getName()  != null ? u.getName()  : "Unknown",
                    u.getEmail() != null ? u.getEmail() : "",
                    roleName,
                    enquiries,
                    customers,
                    quotations,
                    invoices,
                    revenue
            );
        }).toList();

        log.info("=== STAFF SUMMARY REQUEST END — {} records ===", result.size());
        return result;
    }

    private double orZero(Double value) {
        return value != null ? value : 0.0;
    }
}
