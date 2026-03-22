package com.satyam.quotation.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.satyam.quotation.dto.QuotationDTO;
import com.satyam.quotation.dto.QuotationRequestDTO;
import com.satyam.quotation.model.Quotation;

@Mapper(componentModel = "spring", uses = {QuotationItemMapper.class})
public interface QuotationMapper {

    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "customer.customerName", target = "customerName")
    @Mapping(source = "createdBy", target = "createdBy")
    @Mapping(target = "createdByName", ignore = true)
    @Mapping(source = "createdAt", target = "createdAt")
    QuotationDTO toDto(Quotation quotation);
    
    @Mapping(source = "customerId", target = "customer.id")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "quotationNumber", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "subtotal", ignore = true)
    @Mapping(target = "totalDiscount", ignore = true)
    @Mapping(target = "totalGst", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "pdfPath", ignore = true)
    @Mapping(target = "emailSent", ignore = true)
    @Mapping(target = "emailSentAt", ignore = true)
    @Mapping(target = "emailStatus", ignore = true)
    @Mapping(target = "emailErrorMessage", ignore = true)
    @Mapping(target = "lastReminderSentAt", ignore = true)
    Quotation toEntity(QuotationRequestDTO requestDTO);
}
