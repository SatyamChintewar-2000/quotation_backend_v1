package com.satyam.quotation.service.impl;

import com.satyam.quotation.service.SmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class SmsServiceImpl implements SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsServiceImpl.class);

    private final boolean smsEnabled;
    private final String provider;
    private final String twilioAccountSid;
    private final String twilioAuthToken;
    private final String twilioFromNumber;
    private final HttpClient httpClient;

    public SmsServiceImpl(
            @Value("${app.sms.enabled:false}") boolean smsEnabled,
            @Value("${sms.provider:twilio}") String provider,
            @Value("${sms.twilio.account-sid:}") String twilioAccountSid,
            @Value("${sms.twilio.auth-token:}") String twilioAuthToken,
            @Value("${sms.twilio.from-number:}") String twilioFromNumber) {
        this.smsEnabled = smsEnabled;
        this.provider = provider;
        this.twilioAccountSid = twilioAccountSid;
        this.twilioAuthToken = twilioAuthToken;
        this.twilioFromNumber = twilioFromNumber;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public boolean isSmsEnabled() {
        return smsEnabled
                && ("twilio".equalsIgnoreCase(provider) || "mock".equalsIgnoreCase(provider))
                && ("mock".equalsIgnoreCase(provider)
                        || (!twilioAccountSid.isBlank() && !twilioAuthToken.isBlank() && !twilioFromNumber.isBlank()));
    }

    @Override
    public boolean isMockProvider() {
        return "mock".equalsIgnoreCase(provider);
    }

    @Override
    public void sendSms(String phoneNumber, String message) {
        if (!isSmsEnabled()) {
            throw new RuntimeException("SMS service is not configured");
        }

        if ("mock".equalsIgnoreCase(provider)) {
            log.info("📱 [MOCK SMS] Sending to {}: {}", phoneNumber, message);
            return;
        }

        if (!"twilio".equalsIgnoreCase(provider)) {
            throw new RuntimeException("Unsupported SMS provider: " + provider);
        }

        try {
            String form = "To=" + URLEncoder.encode(phoneNumber, StandardCharsets.UTF_8)
                    + "&From=" + URLEncoder.encode(twilioFromNumber, StandardCharsets.UTF_8)
                    + "&Body=" + URLEncoder.encode(message, StandardCharsets.UTF_8);

            String authorization = Base64.getEncoder().encodeToString(
                    (twilioAccountSid + ":" + twilioAuthToken).getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(String.format("https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json",
                            twilioAccountSid)))
                    .header("Authorization", "Basic " + authorization)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new RuntimeException("SMS send failed: " + response.statusCode() + " " + response.body());
            }
            log.info("SMS sent successfully to {}", phoneNumber);
        } catch (Exception e) {
            log.error("Failed to send SMS to {}", phoneNumber, e);
            String rootMessage = e.getMessage() != null ? e.getMessage() : "Failed to send SMS";
            throw new RuntimeException(rootMessage, e);
        }
    }
}
