package com.satyam.quotation.repository;

import com.satyam.quotation.model.EmailLog;
import com.satyam.quotation.model.Quotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
    
    List<EmailLog> findByQuotation(Quotation quotation);
    
    List<EmailLog> findByQuotationOrderBySentAtDesc(Quotation quotation);
    
    List<EmailLog> findByStatus(EmailLog.EmailStatus status);
    
    List<EmailLog> findByEmailType(EmailLog.EmailType emailType);
}
