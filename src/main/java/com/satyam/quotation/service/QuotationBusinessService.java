package com.satyam.quotation.service;

import com.satyam.quotation.model.Quotation;
import com.satyam.quotation.model.QuotationItem;

import java.util.List;

public interface QuotationBusinessService {
    
    /**
     * Generate unique quotation number for company
     * Format: QT-{companyId}-{sequence}
     */
    String generateQuotationNumber(Long companyId);
    
    /**
     * Validate if quotation can be edited
     * Only DRAFT status allows editing
     */
    boolean canEdit(Quotation quotation);
    
    /**
     * Validate if quotation can change to target status
     */
    boolean canChangeStatus(Quotation quotation, String targetStatus);
    
    /**
     * Check if quotation is expired
     */
    boolean isExpired(Quotation quotation);
    
    /**
     * Calculate quotation totals from items
     */
    void calculateTotals(Quotation quotation);
    
    /**
     * Capture product snapshot for quotation item
     */
    void captureProductSnapshot(QuotationItem item);
    
    /**
     * Duplicate quotation (for revising rejected quotations)
     */
    Quotation duplicateQuotation(Long quotationId, Long userId);
    
    /**
     * Validate quotation before status change
     */
    void validateQuotation(Quotation quotation, String targetStatus);
    
    /**
     * Get quotations by status
     */
    List<Quotation> getQuotationsByStatus(String status, Long companyId);
    
    /**
     * Get expired quotations
     */
    List<Quotation> getExpiredQuotations(Long companyId);
}
