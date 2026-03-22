package com.satyam.quotation.service;

import com.satyam.quotation.model.Quotation;

import java.util.List;
import java.util.Optional;

public interface QuotationService {
    
    Quotation createQuotation(Quotation quotation);
    
    Optional<Quotation> getQuotationById(Long id);
    
    List<Quotation> getQuotationsByCompany(Long companyId);
    
    List<Quotation> getQuotationsByUser(Long userId);
    
    List<Quotation> getAllQuotations();
    
    Quotation updateQuotation(Long id, Quotation quotation, Long userId);
    
    void deleteQuotation(Long id, Long userId);
    
    Double getRevenueByCompany(Long companyId);
    
    Double getRevenueByUser(Long userId);
    
    // New business logic methods
    Quotation duplicateQuotation(Long quotationId, Long userId);
    
    Quotation changeStatus(Long quotationId, String newStatus, Long userId);
    
    List<Quotation> getQuotationsByStatus(String status, Long companyId);
    
    List<Quotation> getExpiredQuotations(Long companyId);
}
