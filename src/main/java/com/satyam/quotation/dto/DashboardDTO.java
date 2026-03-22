package com.satyam.quotation.dto;

import lombok.Data;

@Data
public class DashboardDTO {
    private Long customers;
    private Long quotations;
    private Long products;
    private Long staff;
    private Long users;
    private Double revenue;
}

