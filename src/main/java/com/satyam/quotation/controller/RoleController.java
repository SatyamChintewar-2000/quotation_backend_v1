package com.satyam.quotation.controller;

import com.satyam.quotation.dto.RoleDTO;
import com.satyam.quotation.repository.RoleRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleRepository roleRepository;

    public RoleController(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @GetMapping
    public List<RoleDTO> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(role -> {
                    RoleDTO dto = new RoleDTO();
                    dto.setId(role.getId());
                    dto.setRoleName(role.getRoleName());
                    return dto;
                })
                .toList();
    }
}
