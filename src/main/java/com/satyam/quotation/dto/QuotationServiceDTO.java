package com.satyam.quotation.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class QuotationServiceDTO {
    private Long id;
    private String serviceName;
    private BigDecimal servicePrice;
    private BigDecimal serviceTax;
}
