package com.satyam.quotation.mapper;

import com.satyam.quotation.dto.EnquiryDTO;
import com.satyam.quotation.dto.EnquiryRequestDTO;
import com.satyam.quotation.model.Enquiry;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface EnquiryMapper {

    @Mapping(source = "company.id", target = "companyId")
    @Mapping(source = "convertedCustomer.id", target = "convertedCustomerId")
    EnquiryDTO toDto(Enquiry enquiry);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "convertedCustomer", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    Enquiry toEntity(EnquiryRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "convertedCustomer", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    void updateEntity(EnquiryRequestDTO dto, @MappingTarget Enquiry enquiry);
}
