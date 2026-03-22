package com.satyam.quotation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserRequestDTO {
    
    @NotBlank(message = "Name is required")
    private String name;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    private String phone;
    
    private String countryCode = "+91";
    
    @NotNull(message = "Role ID is required")
    private Long roleId;
    
    private String department;
    
    private Boolean active = true;
    
    private String password;
    
    private Long companyId;  // For SUPER_ADMIN to assign company when creating CLIENT users
}
