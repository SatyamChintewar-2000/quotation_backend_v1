package com.satyam.quotation.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.satyam.quotation.dto.CompanyDTO;
import com.satyam.quotation.dto.CompanyRequestDTO;
import com.satyam.quotation.model.Company;


@Mapper(componentModel = "spring")
public interface CompanyMapper {

    CompanyDTO toDto(Company company);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "logo", ignore = true)
    Company toEntity(CompanyRequestDTO requestDTO);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "logo", ignore = true)
    void updateEntity(CompanyRequestDTO requestDTO, @MappingTarget Company company);
}
