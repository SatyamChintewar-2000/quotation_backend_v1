package com.satyam.quotation.service;

import com.satyam.quotation.model.Customer;

import java.util.List;
import java.util.Optional;

public interface CustomerService {
    Customer createCustomer(Customer customer, Long userId, Long companyId);
    List<Customer> getCustomersByCompany(Long companyId);
    List<Customer> getCustomersByUser(Long userId);
    Optional<Customer> getCustomerById(Long id);
    Customer updateCustomer(Customer customer, Long userId);
    void deleteCustomer(Long id, Long userId);
}
