package com.satyam.quotation.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class EnquiryDTO {
    private Long id;
    private LocalDate enquiryDate;
    private String name;
    private String contact;
    private String email;
    private String gender;
    private LocalDate birthDate;
    private BigDecimal budget;
    private String address;
    private String enquiryFor;
    private String rating;
    private String status;
    private String city;
    private String referType;
    private String referBy;
    private LocalDate nextFollowupDate;
    private String comment;
    private Long convertedCustomerId;
    private Long companyId;
    private LocalDateTime createdAt;
}
