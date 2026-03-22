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
}
