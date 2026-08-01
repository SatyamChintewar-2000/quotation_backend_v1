package com.satyam.quotation.service.impl;

import com.satyam.quotation.model.Customer;
import com.satyam.quotation.model.Enquiry;
import com.satyam.quotation.repository.CompanyRepository;
import com.satyam.quotation.repository.CustomerRepository;
import com.satyam.quotation.repository.EnquiryRepository;
import com.satyam.quotation.service.EnquiryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class EnquiryServiceImpl implements EnquiryService {

    private static final Logger log = LoggerFactory.getLogger(EnquiryServiceImpl.class);

    private final EnquiryRepository enquiryRepository;
    private final CompanyRepository companyRepository;
    private final CustomerRepository customerRepository;
    private final CacheManager cacheManager;

    public EnquiryServiceImpl(EnquiryRepository enquiryRepository,
                               CompanyRepository companyRepository,
                               CustomerRepository customerRepository,
                               CacheManager cacheManager) {
        this.enquiryRepository = enquiryRepository;
        this.companyRepository = companyRepository;
        this.customerRepository = customerRepository;
        this.cacheManager = cacheManager;
    }

    // Evict only the specific company's enquiry cache — other companies unaffected
    private void evictEnquiryCache(Long companyId, Long createdBy) {
        var cache = cacheManager.getCache("enquiries");
        if (cache != null) {
            if (companyId != null) cache.evict("company:" + companyId);
            if (createdBy  != null) cache.evict("user:"    + createdBy);
            cache.evict("all");
        }
    }

    @Override
    @Transactional
    public Enquiry create(Enquiry enquiry, Long userId, Long companyId) {
        log.info("=== CREATE ENQUIRY START ===");
        log.info("Status received: '{}'", enquiry.getStatus());
        
        // Trim status to remove any whitespace
        if (enquiry.getStatus() != null) {
            enquiry.setStatus(enquiry.getStatus().trim());
        }
        
        enquiry.setCreatedBy(userId);
        enquiry.setCreatedAt(LocalDateTime.now());
        enquiry.setUpdatedAt(LocalDateTime.now());
        enquiry.setCompany(
            companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"))
        );

        Enquiry saved = enquiryRepository.save(enquiry);
        
        log.info("After save - Status: '{}'", saved.getStatus());
        log.info("Status equals 'converted': {}", "converted".equals(saved.getStatus()));

        // Auto-convert to customer ONLY if status is EXACTLY 'converted'
        if ("converted".equals(saved.getStatus())) {
            log.info("=== CONDITION MET: Converting to customer on create ===");
            convertToCustomer(saved, userId, companyId);
        } else {
            log.info("=== CONDITION NOT MET: Skipping customer conversion on create ===");
        }
        
        log.info("=== CREATE ENQUIRY END ===");
        evictEnquiryCache(saved.getCompany() != null ? saved.getCompany().getId() : null, userId);
        return saved;
    }

    @Override
    @Cacheable(value = "enquiries", key = "'all'")
    public List<Enquiry> getAll() {
        return enquiryRepository.findAllActive();
    }

    @Override
    @Cacheable(value = "enquiries", key = "'company:' + #companyId")
    public List<Enquiry> getByCompany(Long companyId) {
        return enquiryRepository.findByCompanyIdAndDeletedAtIsNull(companyId);
    }

    @Override
    @Cacheable(value = "enquiries", key = "'user:' + #userId")
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
        log.info("=== UPDATE ENQUIRY START ===");
        log.info("Enquiry ID: {}", enquiry.getId());
        log.info("Status received: '{}'", enquiry.getStatus());
        log.info("Status length: {}", enquiry.getStatus() != null ? enquiry.getStatus().length() : 0);
        log.info("Already has customer: {}", enquiry.getConvertedCustomer() != null);
        
        // Trim status to remove any whitespace
        if (enquiry.getStatus() != null) {
            enquiry.setStatus(enquiry.getStatus().trim());
        }
        
        enquiry.setUpdatedBy(userId);
        enquiry.setUpdatedAt(LocalDateTime.now());

        Enquiry saved = enquiryRepository.save(enquiry);
        
        log.info("After save - Status: '{}'", saved.getStatus());
        log.info("Status equals 'converted': {}", "converted".equals(saved.getStatus()));
        log.info("Status equalsIgnoreCase 'converted': {}", "converted".equalsIgnoreCase(saved.getStatus()));
        log.info("Already has customer after save: {}", saved.getConvertedCustomer() != null);

        // Auto-convert ONLY when status is EXACTLY 'converted' and not already converted
        if ("converted".equals(saved.getStatus()) && saved.getConvertedCustomer() == null) {
            log.info("=== CONDITION MET: Converting to customer ===");
            // Get company ID from the saved enquiry
            Long companyId = saved.getCompany() != null ? saved.getCompany().getId() : null;
            if (companyId != null) {
                convertToCustomer(saved, userId, companyId);
                // Refresh to get the updated convertedCustomer
                saved = enquiryRepository.findById(saved.getId()).orElse(saved);
            } else {
                log.warn("Cannot convert: company ID is null");
            }
        } else {
            log.info("=== CONDITION NOT MET: Skipping customer conversion ===");
            log.info("Reason: status='{}', hasCustomer={}", saved.getStatus(), saved.getConvertedCustomer() != null);
        }
        
        log.info("=== UPDATE ENQUIRY END ===");
        evictEnquiryCache(saved.getCompany() != null ? saved.getCompany().getId() : null, userId);
        return saved;
    }

    @Override
    public void delete(Long id, Long userId) {
        Enquiry enquiry = enquiryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Enquiry not found"));

        Long companyId = enquiry.getCompany() != null ? enquiry.getCompany().getId() : null;
        Long createdBy = enquiry.getCreatedBy();

        enquiry.setDeletedAt(LocalDateTime.now());
        enquiry.setDeletedBy(userId);
        enquiryRepository.save(enquiry);

        evictEnquiryCache(companyId, createdBy);
    }

    private void convertToCustomer(Enquiry enquiry, Long userId, Long companyId) {
        log.info("Converting enquiry {} to customer. Status: {}, Already converted: {}", 
                 enquiry.getId(), enquiry.getStatus(), enquiry.getConvertedCustomer() != null);
        
        // Don't create duplicate customer
        if (enquiry.getConvertedCustomer() != null) {
            log.info("Enquiry {} already has a converted customer (ID: {}), skipping conversion", 
                     enquiry.getId(), enquiry.getConvertedCustomer().getId());
            return;
        }

        log.info("Creating customer from enquiry: name={}, email={}, phone={}", 
                 enquiry.getName(), enquiry.getEmail(), enquiry.getContact());

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
        log.info("Customer created successfully with ID: {}", savedCustomer.getId());

        enquiry.setConvertedCustomer(savedCustomer);
        enquiryRepository.save(enquiry);
        log.info("Enquiry {} updated with converted customer ID: {}", enquiry.getId(), savedCustomer.getId());
    }
}
