package com.satyam.quotation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CompanyRequestDTO {
    @NotBlank(message = "Company name is required")
    private String companyName;

    private String address;
    private String phone;
    private String email;
    private String gstNumber;
    private String state;
    private String city;
    private String termsAndConditions;
    private String logo;
    
    // Bank details for payment
    private String bankName;
    private String accountNumber;
    private String ifscCode;
    private String branchName;
    private String upiId;

    // Feature 1: PDF Theme (client can change)
    private String pdfThemeName;
    private String pdfAccentColor;
    private Boolean pdfWatermarkEnabled;
    private java.math.BigDecimal pdfWatermarkOpacity;

    // Subscription expiry (Super Admin sets this)
    private java.time.LocalDate subscriptionExpiresAt;
}
