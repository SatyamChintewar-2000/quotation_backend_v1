package com.satyam.quotation.repository;

public interface DashboardRepository {

    Long countUsersByCompany(Long companyId);

    Long countCustomersByCompany(Long companyId);

    Long countQuotationsByCompany(Long companyId);
}
