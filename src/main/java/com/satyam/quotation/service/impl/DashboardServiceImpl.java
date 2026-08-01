package com.satyam.quotation.service.impl;

import com.satyam.quotation.repository.CustomerRepository;
import com.satyam.quotation.repository.ProductRepository;
import com.satyam.quotation.repository.QuotationRepository;
import com.satyam.quotation.repository.UserRepository;
import com.satyam.quotation.service.DashboardService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final QuotationRepository quotationRepository;
    private final ProductRepository productRepository;

    public DashboardServiceImpl(UserRepository userRepository,
                                CustomerRepository customerRepository,
                                QuotationRepository quotationRepository,
                                ProductRepository productRepository) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.quotationRepository = quotationRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Cacheable(value = "dashboard", key = "'role:' + #role + ':id:' + #userId")
    public Map<String, Object> getDashboardData(Long userId, String role, Long companyId) {

        Map<String, Object> response = new HashMap<>();

        switch (role) {

            case "SUPER_ADMIN" -> {
                response.put("users", userRepository.count());
                response.put("customers", customerRepository.count());
                response.put("quotations", quotationRepository.count());
                response.put("products", productRepository.count());
            }

            case "CLIENT" -> {
                response.put("staff", userRepository.countByCompanyIdAndActiveTrue(companyId));
                response.put("customers", customerRepository.countByCompanyIdAndActiveTrue(companyId));
                response.put("quotations", quotationRepository.findByCompanyId(companyId).size());
                response.put("products", productRepository.countByCompanyIdAndActiveTrue(companyId));
                response.put("revenue", quotationRepository.getTotalRevenueByCompany(companyId));
            }

            case "STAFF" -> {
                // Use COUNT queries instead of full table scans + Java filter
                response.put("customers", customerRepository.countByCreatedByAndActiveTrue(userId));
                response.put("quotations", quotationRepository.countByCreatedByAndActive(userId));
                response.put("products", productRepository.countByCreatedByAndActiveTrue(userId));
                response.put("revenue", quotationRepository.getTotalRevenueByUser(userId));
            }
        }

        return response;
    }
}
