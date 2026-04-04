package com.satyam.quotation.controller;

import com.satyam.quotation.dto.EnquiryDTO;
import com.satyam.quotation.dto.EnquiryRequestDTO;
import com.satyam.quotation.mapper.EnquiryMapper;
import com.satyam.quotation.security.CustomUserDetails;
import com.satyam.quotation.service.EnquiryService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/enquiries")
public class EnquiryController {

    private static final Logger log = LoggerFactory.getLogger(EnquiryController.class);

    private final EnquiryService enquiryService;
    private final EnquiryMapper enquiryMapper;

    public EnquiryController(EnquiryService enquiryService, EnquiryMapper enquiryMapper) {
        this.enquiryService = enquiryService;
        this.enquiryMapper = enquiryMapper;
    }

    @PostMapping
    public EnquiryDTO create(@Valid @RequestBody EnquiryRequestDTO request,
                              Authentication authentication) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        log.info("User {} creating enquiry for {}", user.getUserId(), request.getName());
        var enquiry = enquiryMapper.toEntity(request);
        var saved = enquiryService.create(enquiry, user.getUserId(), user.getCompanyId());
        return enquiryMapper.toDto(saved);
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<EnquiryDTO> getAll(Authentication authentication) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        String role = user.getRole();

        // Superadmin sees everything
        if ("SUPERADMIN".equalsIgnoreCase(role) || "SUPER_ADMIN".equalsIgnoreCase(role)) {
            return enquiryService.getAll()
                .stream().map(enquiryMapper::toDto).toList();
        }

        // Admin/Client sees their whole company
        if ("ADMIN".equalsIgnoreCase(role) || "CLIENT".equalsIgnoreCase(role)) {
            return enquiryService.getByCompany(user.getCompanyId())
                .stream().map(enquiryMapper::toDto).toList();
        }

        // Staff sees only their own records
        return enquiryService.getByUser(user.getUserId())
            .stream().map(enquiryMapper::toDto).toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public EnquiryDTO getById(@PathVariable Long id) {
        return enquiryService.getById(id)
            .map(enquiryMapper::toDto)
            .orElseThrow(() -> new RuntimeException("Enquiry not found: " + id));
    }

    @PutMapping("/{id}")
    public EnquiryDTO update(@PathVariable Long id,
                              @Valid @RequestBody EnquiryRequestDTO request,
                              Authentication authentication) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        log.info("User {} updating enquiry {}", user.getUserId(), id);
        var enquiry = enquiryService.getById(id)
            .orElseThrow(() -> new RuntimeException("Enquiry not found: " + id));
        enquiryMapper.updateEntity(request, enquiry);
        return enquiryMapper.toDto(enquiryService.update(enquiry, user.getUserId()));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, Authentication authentication) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        log.info("User {} deleting enquiry {}", user.getUserId(), id);
        enquiryService.delete(id, user.getUserId());
    }
}
