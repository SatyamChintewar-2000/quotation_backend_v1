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
}
