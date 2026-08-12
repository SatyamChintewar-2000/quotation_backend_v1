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

    // CBM / Weight / USD export column toggles (V28)
    private Boolean showWeightColumn;
    private Boolean showCbmColumn;
    private Boolean showUsdColumn;
    private java.math.BigDecimal ratePerCbm;
    private java.math.BigDecimal usdExchangeRate;

    // Advanced Options (CBM) — V30
    private Boolean cbmAdvancedMode;
    private java.math.BigDecimal shippingCostUsd;
    private java.math.BigDecimal clearancePerCbm;
    private java.math.BigDecimal installationCost;
}
