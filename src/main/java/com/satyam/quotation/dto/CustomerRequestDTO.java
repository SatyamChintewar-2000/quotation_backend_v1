package com.satyam.quotation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CustomerRequestDTO {

    @NotBlank
    private String customerName;

    @Email
    private String email;
    
    private String phone;
    
    private String address;
}
