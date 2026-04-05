package com.satyam.quotation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaffSummaryDTO {
    private Long userId;
    private String userName;
    private String email;
    private String role;
    private long enquiries;
    private long customers;
    private long quotations;
    private long invoices;
    private double totalRevenue;
}
