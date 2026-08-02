package com.satyam.quotation.service;

public interface SmsService {

    boolean isSmsEnabled();

    boolean isMockProvider();

    void sendSms(String phoneNumber, String message);
}
