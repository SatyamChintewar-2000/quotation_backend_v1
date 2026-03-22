package com.satyam.quotation.service;

import com.satyam.quotation.model.Quotation;

public interface WhatsAppService {
    void sendQuotationNotification(Quotation quotation, String message);
    void sendStatusChangeNotification(Quotation quotation, String oldStatus, String newStatus);
}
