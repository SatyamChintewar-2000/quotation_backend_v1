package com.satyam.quotation.dto;

import lombok.Data;

@Data
public class CompanyDTO {
    private Long id;
    private String companyName;
    private String address;
    private String phone;
    private String email;
    private String gstNumber;
    private String state;
    private String city;
    private String termsAndConditions;
    private String logo;
    private Boolean active;
    
    // Bank details for payment
    private String bankName;
    private String accountNumber;
    private String ifscCode;
    private String branchName;
    private String upiId;

    // Feature 1: PDF Theme
    private String pdfThemeName;
    private String pdfAccentColor;
    private Boolean pdfWatermarkEnabled;
    private java.math.BigDecimal pdfWatermarkOpacity;

    // Feature 3: Lock Company Name (read-only in DTO — set by backend only)
    private Boolean companyNameLocked;

    // License ID — read-only, auto-generated, never changes
    private String licenseId;
}
