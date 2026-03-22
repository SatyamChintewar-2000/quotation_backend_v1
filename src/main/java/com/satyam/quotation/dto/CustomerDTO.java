package com.satyam.quotation.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CustomerDTO {
    private Long id;
    private String customerName;
    private String email;
    private String phone;
    private String address;
    private String gstNumber;
    private Long companyId;
    private String companyName;
    private Long createdBy;
    private String createdByName;
}

