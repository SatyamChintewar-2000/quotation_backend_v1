package com.satyam.quotation.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.satyam.quotation.model.Customer;
import com.satyam.quotation.repository.CompanyRepository;
import com.satyam.quotation.repository.CustomerRepository;
import com.satyam.quotation.repository.UserRepository;
import com.satyam.quotation.service.CustomerService;

@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final CacheManager cacheManager;

    public CustomerServiceImpl(CustomerRepository customerRepository,
                               UserRepository userRepository,
                               CompanyRepository companyRepository,
                               CacheManager cacheManager) {
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.cacheManager = cacheManager;
    }

    private void evictCustomerCache(Long companyId, Long createdBy) {
        var cache = cacheManager.getCache("customers");
        if (cache != null) {
            if (companyId != null) cache.evict("company:" + companyId);
            if (createdBy  != null) cache.evict("user:"    + createdBy);
            cache.evict("all");
        }
    }

    @Override
    public Customer createCustomer(Customer customer, Long userId, Long companyId) {
        // Normalize empty email to null — avoids DB constraint issues with empty strings
        if (customer.getEmail() != null && customer.getEmail().isBlank()) {
            customer.setEmail(null);
        }

        customer.setCreatedBy(userId);

        if (companyId != null) {
            customer.setCompany(
                    companyRepository.findById(companyId)
                            .orElseThrow(() -> new RuntimeException("Company not found"))
            );
        }

        customer.setCreatedAt(LocalDateTime.now());
        customer.setActive(true);

        Customer saved = customerRepository.save(customer);
        evictCustomerCache(companyId, userId);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "customers", key = "'all'")
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "customers", key = "'company:' + #companyId")
    public List<Customer> getCustomersByCompany(Long companyId) {
        return customerRepository.findByCompanyId(companyId);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "customers", key = "'user:' + #userId")
    public List<Customer> getCustomersByUser(Long userId) {
        return customerRepository.findByCreatedBy(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Customer> getCustomerById(Long id) {
        return customerRepository.findById(id);
    }

    @Override
    public Customer updateCustomer(Customer customer, Long userId) {
        // Normalize empty email to null
        if (customer.getEmail() != null && customer.getEmail().isBlank()) {
            customer.setEmail(null);
        }

        customer.setUpdatedBy(userId);
        customer.setUpdatedAt(LocalDateTime.now());
        Customer saved = customerRepository.save(customer);

        Long companyId = saved.getCompany() != null ? saved.getCompany().getId() : null;
        evictCustomerCache(companyId, saved.getCreatedBy());
        return saved;
    }

    @Override
    public void deleteCustomer(Long id, Long userId) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        Long companyId = customer.getCompany() != null ? customer.getCompany().getId() : null;
        Long createdBy = customer.getCreatedBy();

        customer.setActive(false);
        customer.setDeletedAt(LocalDateTime.now());
        customer.setDeletedBy(userId);
        customerRepository.save(customer);

        evictCustomerCache(companyId, createdBy);
    }
}
