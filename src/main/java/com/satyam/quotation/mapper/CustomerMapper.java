package com.satyam.quotation.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.satyam.quotation.dto.CustomerDTO;
import com.satyam.quotation.dto.CustomerRequestDTO;
import com.satyam.quotation.model.Customer;

@Mapper(componentModel = "spring")
public interface CustomerMapper {
    
    @Mapping(source = "company.companyName", target = "companyName")
    @Mapping(source = "company.id", target = "companyId")
    @Mapping(source = "createdBy", target = "createdBy")
    @Mapping(target = "createdByName", ignore = true)
    CustomerDTO toDto(Customer customer);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "gstNumber", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    Customer toEntity(CustomerRequestDTO dto);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "gstNumber", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    void updateEntity(CustomerRequestDTO dto, @MappingTarget Customer customer);
}
