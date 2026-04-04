package com.satyam.quotation.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.satyam.quotation.dto.CompanyDTO;
import com.satyam.quotation.dto.CompanyRequestDTO;
import com.satyam.quotation.mapper.CompanyMapper;
import com.satyam.quotation.model.Company;
import com.satyam.quotation.repository.CompanyRepository;
import com.satyam.quotation.security.CustomUserDetails;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private static final Logger log = LoggerFactory.getLogger(CompanyController.class);

    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;

    public CompanyController(CompanyRepository companyRepository,
                            CompanyMapper companyMapper) {
        this.companyRepository = companyRepository;
        this.companyMapper = companyMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyDTO createCompany(
            @Valid @RequestBody CompanyRequestDTO request,
            Authentication authentication) {

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();

        log.info("User {} creating company {}", user.getUserId(), request.getCompanyName());

        // Only SUPER_ADMIN can create companies
        if (!"SUPER_ADMIN".equals(user.getRole())) {
            throw new RuntimeException("Only SUPER_ADMIN can create companies");
        }

        Company company = companyMapper.toEntity(request);
        company.setActive(true);
        company.setCreatedAt(LocalDateTime.now());

        Company savedCompany = companyRepository.save(company);

        return companyMapper.toDto(savedCompany);
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<CompanyDTO> getCompanies(Authentication authentication) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        // Superadmin sees all companies (active + inactive) to manage them
        boolean isSuperAdmin = "SUPER_ADMIN".equals(user.getRole()) || "SUPERADMIN".equals(user.getRole());
        return companyRepository.findAll().stream()
                .filter(c -> isSuperAdmin || Boolean.TRUE.equals(c.getActive()))
                .map(companyMapper::toDto)
                .toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public CompanyDTO getCompany(@PathVariable Long id) {
        return companyRepository.findById(id)
                .filter(Company::getActive)
                .map(companyMapper::toDto)
                .orElseThrow(() -> new com.satyam.quotation.exception.ResourceNotFoundException(
                        "Company not found with id: " + id));
    }

    @PutMapping("/{id}")
    public CompanyDTO updateCompany(
            @PathVariable Long id,
            @Valid @RequestBody CompanyRequestDTO request,
            Authentication authentication) {

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();

        log.info("User {} updating company {}", user.getUserId(), id);

        // Only SUPER_ADMIN can update companies
        if (!"SUPER_ADMIN".equals(user.getRole())) {
            throw new RuntimeException("Only SUPER_ADMIN can update companies");
        }

        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new com.satyam.quotation.exception.ResourceNotFoundException(
                        "Company not found with id: " + id));

        companyMapper.updateEntity(request, company);
        company.setUpdatedAt(LocalDateTime.now());

        Company updatedCompany = companyRepository.save(company);

        return companyMapper.toDto(updatedCompany);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCompany(
            @PathVariable Long id,
            Authentication authentication) {

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();

        log.info("User {} deleting company {}", user.getUserId(), id);

        if (!"SUPER_ADMIN".equals(user.getRole()) && !"SUPERADMIN".equals(user.getRole())) {
            throw new RuntimeException("Only SUPER_ADMIN can delete companies");
        }

        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new com.satyam.quotation.exception.ResourceNotFoundException(
                        "Company not found with id: " + id));

        company.setActive(false);
        company.setDeletedAt(LocalDateTime.now());

        companyRepository.save(company);
    }

    @PutMapping("/{id}/toggle-active")
    public CompanyDTO toggleActive(
            @PathVariable Long id,
            Authentication authentication) {

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();

        if (!"SUPER_ADMIN".equals(user.getRole()) && !"SUPERADMIN".equals(user.getRole())) {
            throw new RuntimeException("Only SUPER_ADMIN can activate/deactivate companies");
        }

        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new com.satyam.quotation.exception.ResourceNotFoundException(
                        "Company not found with id: " + id));

        company.setActive(!Boolean.TRUE.equals(company.getActive()));
        company.setUpdatedAt(LocalDateTime.now());
        company.setUpdatedBy(user.getUserId());

        log.info("User {} toggled company {} active to {}", user.getUserId(), id, company.getActive());

        return companyMapper.toDto(companyRepository.save(company));
    }
}