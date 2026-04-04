package com.satyam.quotation.controller;

import com.satyam.quotation.dto.StaffSummaryDTO;
import com.satyam.quotation.exception.UnauthorizedException;
import com.satyam.quotation.repository.CustomerRepository;
import com.satyam.quotation.repository.EnquiryRepository;
import com.satyam.quotation.repository.InvoiceRepository;
import com.satyam.quotation.repository.QuotationRepository;
import com.satyam.quotation.repository.UserRepository;
import com.satyam.quotation.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportsController {

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
        CustomUserDetails currentUser = (CustomUserDetails) authentication.getPrincipal();
        String role = currentUser.getRole();

        // Only admin and superadmin can access this
        boolean isAdmin = "ADMIN".equalsIgnoreCase(role)
                || "CLIENT".equalsIgnoreCase(role)
                || "SUPERADMIN".equalsIgnoreCase(role)
                || "SUPER_ADMIN".equalsIgnoreCase(role);

        if (!isAdmin) {
            throw new UnauthorizedException("Access denied");
        }

        Long companyId = currentUser.getCompanyId();

        // Get all staff users for this company
        List<com.satyam.quotation.model.User> staffUsers = userRepository.findByCompanyId(companyId)
                .stream()
                .filter(u -> u.getActive() != null && u.getActive())
                .toList();

        return staffUsers.stream().map(u -> {
            long enquiries = enquiryRepository.findByCreatedByAndDeletedAtIsNull(u.getId()).size();
            long customers = customerRepository.findByCreatedBy(u.getId())
                    .stream().filter(c -> c.getActive() != null && c.getActive()).count();
            long quotations = quotationRepository.findByCreatedBy(u.getId()).size();
            long invoices = invoiceRepository.findAll().stream()
                    .filter(i -> u.getId().equals(i.getCreatedBy()) && Boolean.TRUE.equals(i.getActive()))
                    .count();
            Double revenue = quotationRepository.getTotalRevenueByUser(u.getId());

            return new StaffSummaryDTO(
                    u.getId(),
                    u.getName(),
                    u.getEmail(),
                    u.getRole() != null ? u.getRole().getRoleName() : "staff",
                    enquiries,
                    customers,
                    quotations,
                    invoices,
                    revenue != null ? revenue : 0.0
            );
        }).toList();
    }
}
