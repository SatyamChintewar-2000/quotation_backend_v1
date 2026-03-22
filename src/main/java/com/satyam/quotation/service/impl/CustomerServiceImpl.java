package com.satyam.quotation.service.impl;

import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;

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

    public CustomerServiceImpl(CustomerRepository customerRepository,
                               UserRepository userRepository,
                               CompanyRepository companyRepository) {
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
    }

    @Override
    public Customer createCustomer(Customer customer, Long userId, Long companyId) {

        // Validate phone number is unique within company
        if (customer.getPhone() != null && companyId != null) {
            boolean phoneExists = customerRepository.findByCompanyId(companyId)
                    .stream()
                    .anyMatch(c -> c.getPhone().equals(customer.getPhone()) && c.getActive());
            
            if (phoneExists) {
                throw new RuntimeException("Customer with this phone number already exists in your company");
            }
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

        return customerRepository.save(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Customer> getCustomersByCompany(Long companyId) {
        return customerRepository.findByCompanyId(companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Customer> getCustomersByUser(Long userId) {
        return customerRepository.findAll().stream()
                .filter(c -> c.getCreatedBy() != null && 
                           c.getCreatedBy().equals(userId) && 
                           c.getActive())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Customer> getCustomerById(Long id) {
        return customerRepository.findById(id);
    }

    @Override
    public Customer updateCustomer(Customer customer, Long userId) {
        // Validate phone number is unique within company (excluding current customer)
        if (customer.getPhone() != null && customer.getCompany() != null) {
            boolean phoneExists = customerRepository.findByCompanyId(customer.getCompany().getId())
                    .stream()
                    .anyMatch(c -> c.getPhone().equals(customer.getPhone()) && 
                                  c.getActive() && 
                                  !c.getId().equals(customer.getId()));
            
            if (phoneExists) {
                throw new RuntimeException("Customer with this phone number already exists in your company");
            }
        }
        
        customer.setUpdatedBy(userId);
        customer.setUpdatedAt(LocalDateTime.now());
        return customerRepository.save(customer);
    }

    @Override
    public void deleteCustomer(Long id, Long userId) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        
        customer.setActive(false);
        customer.setDeletedAt(LocalDateTime.now());
        customer.setDeletedBy(userId);
        
        customerRepository.save(customer);
    }
}
