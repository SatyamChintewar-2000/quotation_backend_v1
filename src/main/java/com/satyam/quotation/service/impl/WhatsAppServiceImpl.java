package com.satyam.quotation.service.impl;

import com.satyam.quotation.model.Quotation;
import com.satyam.quotation.service.AppSettingsService;
import com.satyam.quotation.service.WhatsAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class WhatsAppServiceImpl implements WhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppServiceImpl.class);

    private final AppSettingsService settingsService;
    private final RestTemplate restTemplate;

    public WhatsAppServiceImpl(AppSettingsService settingsService) {
        this.settingsService = settingsService;
        this.restTemplate = new RestTemplate();
    }

    private boolean isEnabled() {
        return settingsService.getBooleanSetting("whatsapp_notifications_enabled", false);
    }

    @Override
    public void sendQuotationNotification(Quotation quotation, String message) {
        if (!isEnabled()) {
            log.debug("WhatsApp notifications disabled, skipping");
            return;
        }
        String phone = quotation.getCustomer().getPhone();
        if (phone == null || phone.isBlank()) {
            log.warn("No phone number for customer {}, skipping WhatsApp", quotation.getCustomer().getCustomerName());
            return;
        }
        sendMessage(phone, message);
    }

    @Override
    public void sendStatusChangeNotification(Quotation quotation, String oldStatus, String newStatus) {
        if (!isEnabled()) return;

        String message = String.format(
            "Quotation %s status updated: %s → %s\nCustomer: %s\nAmount: ₹%.2f",
            quotation.getQuotationNumber(),
            oldStatus, newStatus,
            quotation.getCustomer().getCustomerName(),
            quotation.getTotalAmount()
        );
        sendQuotationNotification(quotation, message);
    }

    private void sendMessage(String phone, String message) {
        String apiUrl = settingsService.getSetting("whatsapp_api_url", "");
        String token = settingsService.getSetting("whatsapp_api_token", "");
        String phoneNumberId = settingsService.getSetting("whatsapp_phone_number_id", "");

        if (apiUrl.isBlank() || token.isBlank() || phoneNumberId.isBlank()) {
            log.warn("WhatsApp API not configured, skipping message to {}", phone);
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);

            // WhatsApp Cloud API format
            Map<String, Object> body = Map.of(
                "messaging_product", "whatsapp",
                "to", phone,
                "type", "text",
                "text", Map.of("body", message)
            );

            String url = apiUrl + "/" + phoneNumberId + "/messages";
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(url, request, String.class);

            log.info("WhatsApp message sent to {}", phone);
        } catch (Exception e) {
            log.error("Failed to send WhatsApp message to {}: {}", phone, e.getMessage());
        }
    }
}
