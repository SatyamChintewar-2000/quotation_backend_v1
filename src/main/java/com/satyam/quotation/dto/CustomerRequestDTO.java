package com.satyam.quotation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CustomerRequestDTO {

    @NotBlank
    private String customerName;

    // Email is optional — no format validation enforced here.
    // Frontend validates format before submitting; empty string is normalized to null by frontend.
    private String email;

    private String phone;

    private String address;

    private String shippingAddress;
}
