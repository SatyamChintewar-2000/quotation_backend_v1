package com.satyam.quotation.service;

import com.satyam.quotation.model.Quotation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface QuotationService {
    
    Quotation createQuotation(Quotation quotation);

    /**
     * Save address snapshots directly — bypasses edit-status guard.
     * Used immediately after createQuotation to persist customer/shipping address.
     */
    void saveAddressSnapshots(Long quotationId, String customerAddress, String shippingAddress);
    
    Optional<Quotation> getQuotationById(Long id);
    
    List<Quotation> getQuotationsByCompany(Long companyId);
    List<Quotation> getQuotationsByUser(Long userId);
    List<Quotation> getAllQuotations();

    // ── Paginated list (for history page — no images in items) ───────────────
    Page<Quotation> getQuotationsByCompanyPaged(Long companyId, Pageable pageable);
    Page<Quotation> getQuotationsByUserPaged(Long userId, Pageable pageable);
    Page<Quotation> getAllQuotationsPaged(Pageable pageable);
    
    Quotation updateQuotation(Long id, Quotation quotation, Long userId);
    void deleteQuotation(Long id, Long userId);
    
    Double getRevenueByCompany(Long companyId);
    Double getRevenueByUser(Long userId);
    
    Quotation duplicateQuotation(Long quotationId, Long userId);
    Quotation changeStatus(Long quotationId, String newStatus, Long userId);
    List<Quotation> getQuotationsByStatus(String status, Long companyId);
    List<Quotation> getExpiredQuotations(Long companyId);
}
