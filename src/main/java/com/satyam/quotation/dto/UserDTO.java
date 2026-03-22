package com.satyam.quotation.dto;

import lombok.Data;

@Data
public class UserDTO {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String countryCode;
    private Long roleId;
    private String role;
    private String department;
    private Boolean active;
    private Long companyId;
    private String companyName;
    private Long createdBy;
    private String avatar;
}

