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
    private Boolean active;
}
