package com.satyam.quotation.service.impl;

import com.satyam.quotation.model.Customer;
import com.satyam.quotation.model.Enquiry;
import com.satyam.quotation.repository.CompanyRepository;
import com.satyam.quotation.repository.CustomerRepository;
import com.satyam.quotation.repository.EnquiryRepository;
import com.satyam.quotation.service.EnquiryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class EnquiryServiceImpl implements EnquiryService {

    private final EnquiryRepository enquiryRepository;
    private final CompanyRepository companyRepository;
    private final CustomerRepository customerRepository;

    public EnquiryServiceImpl(EnquiryRepository enquiryRepository,
                               CompanyRepository companyRepository,
                               CustomerRepository customerRepository) {
        this.enquiryRepository = enquiryRepository;
        this.companyRepository = companyRepository;
        this.customerRepository = customerRepository;
    }

    @Override
    @Transactional
    public Enquiry create(Enquiry enquiry, Long userId, Long companyId) {
        enquiry.setCreatedBy(userId);
        enquiry.setCreatedAt(LocalDateTime.now());
        enquiry.setUpdatedAt(LocalDateTime.now());
        enquiry.setCompany(
            companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"))
        );

        Enquiry saved = enquiryRepository.save(enquiry);

        // Auto-convert to customer if status is 'converted'
        if ("converted".equalsIgnoreCase(enquiry.getStatus())) {
            convertToCustomer(saved, userId, companyId);
        }

        return saved;
    }

    @Override
    public List<Enquiry> getAll() {
        return enquiryRepository.findAll().stream()
            .filter(e -> e.getDeletedAt() == null)
            .toList();
    }

    @Override
    public List<Enquiry> getByCompany(Long companyId) {
        return enquiryRepository.findByCompanyIdAndDeletedAtIsNull(companyId);
    }

    @Override
    public List<Enquiry> getByUser(Long userId) {
        return enquiryRepository.findByCreatedByAndDeletedAtIsNull(userId);
    }

    @Override
    public Optional<Enquiry> getById(Long id) {
        return enquiryRepository.findById(id);
    }

    @Override
    @Transactional
    public Enquiry update(Enquiry enquiry, Long userId) {
        enquiry.setUpdatedBy(userId);
        enquiry.setUpdatedAt(LocalDateTime.now());

        Enquiry saved = enquiryRepository.save(enquiry);

        // Auto-convert when status changes to 'converted' and not already converted
        if ("converted".equalsIgnoreCase(enquiry.getStatus()) && enquiry.getConvertedCustomer() == null) {
            convertToCustomer(saved, userId, saved.getCompany().getId());
        }

        return saved;
    }

    @Override
    public void delete(Long id, Long userId) {
        Enquiry enquiry = enquiryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Enquiry not found"));
        enquiry.setDeletedAt(LocalDateTime.now());
        enquiry.setDeletedBy(userId);
        enquiryRepository.save(enquiry);
    }

    private void convertToCustomer(Enquiry enquiry, Long userId, Long companyId) {
        // Don't create duplicate customer
        if (enquiry.getConvertedCustomer() != null) return;

        Customer customer = Customer.builder()
            .customerName(enquiry.getName())
            .email(enquiry.getEmail())
            .phone(enquiry.getContact())
            .address(enquiry.getAddress())
            .active(true)
            .createdBy(userId)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .company(enquiry.getCompany())
            .build();

        Customer savedCustomer = customerRepository.save(customer);

        enquiry.setConvertedCustomer(savedCustomer);
        enquiryRepository.save(enquiry);
    }
}
