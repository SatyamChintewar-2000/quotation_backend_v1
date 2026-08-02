package com.satyam.quotation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class EnquiryRequestDTO {

    @NotNull
    private LocalDate enquiryDate;

    @NotBlank
    private String name;

    @NotBlank
    private String contact;

    private String email;
    private String gender;
    private LocalDate birthDate;
    private BigDecimal budget;
    private String address;
    private String enquiryFor;
    private String rating;

    @NotBlank
    private String status;

    private String city;
    private String referType;
    private String referBy;
    private LocalDate nextFollowupDate;
    private String comment;
}
