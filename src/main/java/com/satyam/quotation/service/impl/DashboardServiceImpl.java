package com.satyam.quotation.service.impl;

import com.satyam.quotation.repository.CustomerRepository;
import com.satyam.quotation.repository.ProductRepository;
import com.satyam.quotation.repository.QuotationRepository;
import com.satyam.quotation.repository.UserRepository;
import com.satyam.quotation.service.DashboardService;
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
                response.put("staff", userRepository.findByCompanyId(companyId).size());
                response.put("customers", customerRepository.findByCompanyId(companyId).size());
                response.put("quotations", quotationRepository.findByCompanyId(companyId).size());
                response.put("products", productRepository.findByCompanyId(companyId).size());
                response.put("revenue", quotationRepository.getTotalRevenueByCompany(companyId));
            }

            case "STAFF" -> {
                response.put("customers", customerRepository.findAll().stream()
                        .filter(c -> c.getCreatedBy() != null && 
                                   c.getCreatedBy().equals(userId) && 
                                   c.getActive())
                        .count());
                response.put("quotations", quotationRepository.findAll().stream()
                        .filter(q -> q.getCreatedBy() != null && 
                                   q.getCreatedBy().equals(userId) && 
                                   q.getActive())
                        .count());
                response.put("products", productRepository.findAll().stream()
                        .filter(p -> p.getCreatedBy() != null && 
                                   p.getCreatedBy().equals(userId) && 
                                   p.getActive())
                        .count());
                response.put("revenue", quotationRepository.getTotalRevenueByUser(userId));
            }
        }

        return response;
    }
}
