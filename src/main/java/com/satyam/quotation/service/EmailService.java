package com.satyam.quotation.service;

import com.satyam.quotation.model.EmailLog;
import com.satyam.quotation.model.Quotation;
import com.satyam.quotation.model.User;

import java.io.File;

public interface EmailService {
    
    /**
     * Send quotation email to customer with PDF attachment
     */
    EmailLog sendQuotationEmail(Quotation quotation, File pdfFile, User sentBy);
    
    /**
     * Send quotation approved notification to creator
     */
    EmailLog sendQuotationApprovedEmail(Quotation quotation);
    
    /**
     * Send quotation rejected notification to creator
     */
    EmailLog sendQuotationRejectedEmail(Quotation quotation);
    
    /**
     * Send expiry warning email
     */
    EmailLog sendExpiryWarningEmail(Quotation quotation);
    
    /**
     * Send quotation updated notification
     */
    EmailLog sendQuotationUpdatedEmail(Quotation quotation, User updatedBy);
    
    /**
     * Retry failed email
     */
    EmailLog retryFailedEmail(Long emailLogId);
}
