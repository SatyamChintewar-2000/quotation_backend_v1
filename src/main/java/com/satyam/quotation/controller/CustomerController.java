package com.satyam.quotation.controller;

import com.satyam.quotation.dto.CustomerDTO;
import com.satyam.quotation.dto.CustomerRequestDTO;
import com.satyam.quotation.mapper.CustomerMapper;
import com.satyam.quotation.security.CustomUserDetails;
import com.satyam.quotation.service.CustomerService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private static final Logger log = LoggerFactory.getLogger(CustomerController.class);

    private final CustomerService customerService;
    private final CustomerMapper customerMapper;

    public CustomerController(CustomerService customerService,
                              CustomerMapper customerMapper) {
        this.customerService = customerService;
        this.customerMapper = customerMapper;
    }

    @PostMapping
    public CustomerDTO createCustomer(
            @Valid @RequestBody CustomerRequestDTO request,
            Authentication authentication) {

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();

        log.info("User {} creating customer {}", user.getUserId(), request.getCustomerName());

        var customer = customerMapper.toEntity(request);

        var savedCustomer = customerService.createCustomer(
                customer,
                user.getUserId(),
                user.getCompanyId()
        );

        return customerMapper.toDto(savedCustomer);
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<CustomerDTO> getCustomers(Authentication authentication) {

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        String role = user.getRole();

        // Superadmin sees all customers
        if ("SUPERADMIN".equalsIgnoreCase(role) || "SUPER_ADMIN".equalsIgnoreCase(role)) {
            return customerService.getCustomersByCompany(user.getCompanyId())
                    .stream()
                    .map(customerMapper::toDto)
                    .toList();
        }

        // Admin/Client sees their whole company
        if ("ADMIN".equalsIgnoreCase(role) || "CLIENT".equalsIgnoreCase(role)) {
            return customerService.getCustomersByCompany(user.getCompanyId())
                    .stream()
                    .map(customerMapper::toDto)
                    .toList();
        }

        // Staff sees only their own
        return customerService.getCustomersByUser(user.getUserId())
                .stream()
                .map(customerMapper::toDto)
                .toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public CustomerDTO getCustomer(@PathVariable Long id) {
        var customer = customerService.getCustomerById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
        return customerMapper.toDto(customer);
    }

    @PutMapping("/{id}")
    public CustomerDTO updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerRequestDTO request,
            Authentication authentication) {

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();

        log.info("User {} updating customer {}", user.getUserId(), id);

        var customer = customerService.getCustomerById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));

        customerMapper.updateEntity(request, customer);

        var updatedCustomer = customerService.updateCustomer(customer, user.getUserId());

        return customerMapper.toDto(updatedCustomer);
    }

    @DeleteMapping("/{id}")
    public void deleteCustomer(
            @PathVariable Long id,
            Authentication authentication) {

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();

        log.info("User {} deleting customer {}", user.getUserId(), id);

        customerService.deleteCustomer(id, user.getUserId());
    }
}
