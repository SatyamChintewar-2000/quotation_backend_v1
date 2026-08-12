package com.satyam.quotation.service.impl;

import com.satyam.quotation.exception.ResourceNotFoundException;
import com.satyam.quotation.model.Customer;
import com.satyam.quotation.model.Product;
import com.satyam.quotation.model.Quotation;
import com.satyam.quotation.model.QuotationItem;
import com.satyam.quotation.repository.CustomerRepository;
import com.satyam.quotation.repository.ProductRepository;
import com.satyam.quotation.repository.QuotationRepository;
import com.satyam.quotation.service.QuotationBusinessService;
import com.satyam.quotation.service.QuotationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class QuotationServiceImpl implements QuotationService {

    private static final Logger log = LoggerFactory.getLogger(QuotationServiceImpl.class);

    private final QuotationRepository quotationRepository;
    private final QuotationBusinessService businessService;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final CacheManager cacheManager;

    public QuotationServiceImpl(QuotationRepository quotationRepository,
            QuotationBusinessService businessService,
            ProductRepository productRepository,
            CustomerRepository customerRepository,
            CacheManager cacheManager) {
        this.quotationRepository = quotationRepository;
        this.businessService = businessService;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.cacheManager = cacheManager;
    }

    // ── Targeted cache eviction helpers ──────────────────────────────────────

    /**
     * Evict only the cache entries that belong to the affected company and user.
     * Other companies' cached data is untouched — they keep getting cache hits.
     */
    private void evictQuotationCache(Long companyId, Long createdBy) {
        var cache = cacheManager.getCache("quotations");
        if (cache != null) {
            if (companyId != null) cache.evict("company:" + companyId);
            if (createdBy  != null) cache.evict("user:"    + createdBy);
            cache.evict("all"); // superadmin "all" view must always be refreshed
        }
        // Dashboard and reports stats are per-user/company — evict only affected ones
        var dash = cacheManager.getCache("dashboard");
        if (dash != null) dash.invalidate(); // small cache, safe to clear fully

        var rep = cacheManager.getCache("reports");
        if (rep != null) rep.invalidate();
    }

    // ── Write operations ──────────────────────────────────────────────────────

    @Override
    @Transactional
    public Quotation createQuotation(Quotation quotation) {
        if (quotation.getStatus() == null || quotation.getStatus().isEmpty()) {
            quotation.setStatus("DRAFT");
            log.warn("Status not provided, defaulting to DRAFT");
        } else {
            log.info("Creating quotation with status: {}", quotation.getStatus());
        }

        if (quotation.getCurrency() == null || quotation.getCurrency().isEmpty()) {
            quotation.setCurrency("INR");
        }

        // Default expiry date to 30 days from now if not provided
        if (quotation.getExpiryDate() == null) {
            quotation.setExpiryDate(java.time.LocalDate.now().plusDays(30));
        }

        // Load full customer
        if (quotation.getCustomer() != null && quotation.getCustomer().getId() != null) {
            Customer customer = customerRepository.findById(quotation.getCustomer().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Customer not found with id: " + quotation.getCustomer().getId()));
            quotation.setCustomer(customer);
        }

        // Generate quotation number
        if (quotation.getCompany() != null) {
            String quotationNumber = businessService.generateQuotationNumber(quotation.getCompany().getId());
            quotation.setQuotationNumber(quotationNumber);
            log.info("Generated quotation number: {}", quotationNumber);
        }

        // Link items and load products
        if (quotation.getItems() != null) {
            for (QuotationItem item : quotation.getItems()) {
                item.setQuotation(quotation);
                if (item.getProduct() != null && item.getProduct().getId() != null) {
                    Product product = productRepository.findById(item.getProduct().getId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Product not found with id: " + item.getProduct().getId()));
                    item.setProduct(product);
                }
                businessService.captureProductSnapshot(item);
            }
        }

        // Link services to quotation
        if (quotation.getServices() != null) {
            quotation.getServices().forEach(s -> s.setQuotation(quotation));
        }

        businessService.calculateTotals(quotation);

        quotation.setCreatedAt(LocalDateTime.now());
        quotation.setActive(true);

        // ── Snapshot exchange rate and CBM rate from company settings ─────────
        // Frozen here so old quotations always show the rates in effect at creation.
        if (quotation.getCompany() != null) {
            com.satyam.quotation.model.Company co = quotation.getCompany();
            if (co.getUsdExchangeRate() != null && quotation.getUsdExchangeRateSnapshot() == null) {
                quotation.setUsdExchangeRateSnapshot(co.getUsdExchangeRate());
            }
            if (co.getRatePerCbm() != null && quotation.getRatePerCbmSnapshot() == null) {
                quotation.setRatePerCbmSnapshot(co.getRatePerCbm());
            }
        }

        Quotation saved = quotationRepository.save(quotation);
        log.info("Created quotation: {} with {} items, {} services",
                saved.getQuotationNumber(),
                saved.getItems() != null ? saved.getItems().size() : 0,
                saved.getServices() != null ? saved.getServices().size() : 0);

        // Evict only this company's cache — other clients are unaffected
        evictQuotationCache(
                saved.getCompany() != null ? saved.getCompany().getId() : null,
                saved.getCreatedBy());

        return saved;
    }

    // ── Read operations ───────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "quotations", key = "'id:' + #id")
    public Optional<Quotation> getQuotationById(Long id) {
        return quotationRepository.findByIdWithDetails(id);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "quotations", key = "'company:' + #companyId")
    public List<Quotation> getQuotationsByCompany(Long companyId) {
        return quotationRepository.findByCompanyId(companyId);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "quotations", key = "'user:' + #userId")
    public List<Quotation> getQuotationsByUser(Long userId) {
        return quotationRepository.findByCreatedBy(userId);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "quotations", key = "'all'")
    public List<Quotation> getAllQuotations() {
        return quotationRepository.findAllActive();
    }

    // ── Paginated reads (used by history list endpoint) ───────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<Quotation> getQuotationsByCompanyPaged(Long companyId, Pageable pageable) {
        return quotationRepository.findPageByCompanyId(companyId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Quotation> getQuotationsByUserPaged(Long userId, Pageable pageable) {
        return quotationRepository.findPageByCreatedBy(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Quotation> getAllQuotationsPaged(Pageable pageable) {
        return quotationRepository.findAllActivePage(pageable);
    }

    // ── More write operations ─────────────────────────────────────────────────

    @Override
    @Transactional
    public Quotation updateQuotation(Long id, Quotation updatedQuotation, Long userId) {
        Quotation quotation = quotationRepository.findById(id)
                .filter(Quotation::getActive)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation not found"));

        if (!businessService.canEdit(quotation)) {
            throw new IllegalStateException(
                    String.format("Quotation in %s status cannot be edited", quotation.getStatus()));
        }

        if (updatedQuotation.getStatus() != null &&
                !updatedQuotation.getStatus().equals(quotation.getStatus())) {
            businessService.validateQuotation(quotation, updatedQuotation.getStatus());
        }

        if (updatedQuotation.getStatus() != null)
            quotation.setStatus(updatedQuotation.getStatus());
        if (updatedQuotation.getCustomer() != null)
            quotation.setCustomer(updatedQuotation.getCustomer());
        if (updatedQuotation.getExpiryDate() != null)
            quotation.setExpiryDate(updatedQuotation.getExpiryDate());
        if (updatedQuotation.getQuotationDate() != null)
            quotation.setQuotationDate(updatedQuotation.getQuotationDate());
        if (updatedQuotation.getQuotationCode() != null)
            quotation.setQuotationCode(updatedQuotation.getQuotationCode());
        if (updatedQuotation.getDeliveryDate() != null)
            quotation.setDeliveryDate(updatedQuotation.getDeliveryDate());
        if (updatedQuotation.getExecutiveName() != null)
            quotation.setExecutiveName(updatedQuotation.getExecutiveName());
        if (updatedQuotation.getNotes() != null)
            quotation.setNotes(updatedQuotation.getNotes());
        if (updatedQuotation.getTermsAndConditions() != null)
            quotation.setTermsAndConditions(updatedQuotation.getTermsAndConditions());
        if (updatedQuotation.getDiscountPercentage() != null)
            quotation.setDiscountPercentage(updatedQuotation.getDiscountPercentage());

        quotation.setHideServiceChargesOnPdf(
            updatedQuotation.getHideServiceChargesOnPdf() != null
                ? updatedQuotation.getHideServiceChargesOnPdf()
                : Boolean.FALSE
        );

        if (updatedQuotation.getItems() != null) {
            quotation.getItems().clear();
            for (QuotationItem item : updatedQuotation.getItems()) {
                item.setQuotation(quotation);
                if (item.getProduct() != null && item.getProduct().getId() != null) {
                    Product product = productRepository.findById(item.getProduct().getId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Product not found with id: " + item.getProduct().getId()));
                    item.setProduct(product);
                }
                businessService.captureProductSnapshot(item);
                quotation.getItems().add(item);
            }
        }

        if (updatedQuotation.getServices() != null) {
            quotation.getServices().clear();
            updatedQuotation.getServices().forEach(s -> {
                s.setQuotation(quotation);
                quotation.getServices().add(s);
            });
        }

        businessService.calculateTotals(quotation);
        quotation.setUpdatedAt(LocalDateTime.now());
        quotation.setUpdatedBy(userId);

        Quotation saved = quotationRepository.save(quotation);
        log.info("Updated quotation: {} to status: {}", saved.getQuotationNumber(), saved.getStatus());

        evictQuotationCache(
                saved.getCompany() != null ? saved.getCompany().getId() : null,
                saved.getCreatedBy());

        return saved;
    }

    @Override
    @Transactional
    public void deleteQuotation(Long id, Long userId) {
        Quotation quotation = quotationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation not found"));

        Long companyId  = quotation.getCompany()  != null ? quotation.getCompany().getId() : null;
        Long createdBy  = quotation.getCreatedBy();

        quotation.setActive(false);
        quotation.setDeletedAt(LocalDateTime.now());
        quotation.setDeletedBy(userId);
        quotationRepository.save(quotation);

        // Evict specific entry in id cache too
        var cache = cacheManager.getCache("quotations");
        if (cache != null) cache.evict("id:" + id);

        evictQuotationCache(companyId, createdBy);
    }

    @Override
    public Double getRevenueByCompany(Long companyId) {
        return quotationRepository.getTotalRevenueByCompany(companyId);
    }

    @Override
    public Double getRevenueByUser(Long userId) {
        return quotationRepository.getTotalRevenueByUser(userId);
    }

    @Override
    @Transactional
    public Quotation duplicateQuotation(Long quotationId, Long userId) {
        log.info("Duplicating quotation {} by user {}", quotationId, userId);
        Quotation saved = businessService.duplicateQuotation(quotationId, userId);
        evictQuotationCache(
                saved.getCompany() != null ? saved.getCompany().getId() : null,
                saved.getCreatedBy());
        return saved;
    }

    @Override
    @Transactional
    public Quotation changeStatus(Long quotationId, String newStatus, Long userId) {
        Quotation quotation = quotationRepository.findByIdWithDetails(quotationId)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation not found"));

        businessService.validateQuotation(quotation, newStatus);

        quotation.setStatus(newStatus);
        quotation.setUpdatedAt(LocalDateTime.now());
        quotation.setUpdatedBy(userId);

        Quotation saved = quotationRepository.save(quotation);
        log.info("Changed quotation {} status to {}", saved.getQuotationNumber(), newStatus);

        evictQuotationCache(
                saved.getCompany() != null ? saved.getCompany().getId() : null,
                saved.getCreatedBy());

        return saved;
    }

    @Override
    public List<Quotation> getQuotationsByStatus(String status, Long companyId) {
        return businessService.getQuotationsByStatus(status, companyId);
    }

    @Override
    public List<Quotation> getExpiredQuotations(Long companyId) {
        return businessService.getExpiredQuotations(companyId);
    }
}
