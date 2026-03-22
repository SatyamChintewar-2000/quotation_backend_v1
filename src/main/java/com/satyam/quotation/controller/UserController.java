package com.satyam.quotation.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.satyam.quotation.dto.UserDTO;
import com.satyam.quotation.dto.UserRequestDTO;
import com.satyam.quotation.mapper.UserMapper;
import com.satyam.quotation.model.User;
import com.satyam.quotation.repository.CompanyRepository;
import com.satyam.quotation.repository.RoleRepository;
import com.satyam.quotation.repository.UserRepository;
import com.satyam.quotation.security.CustomUserDetails;
import com.satyam.quotation.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserService userService,
                         UserMapper userMapper,
                         UserRepository userRepository,
                         RoleRepository roleRepository,
                         CompanyRepository companyRepository,
                         PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.companyRepository = companyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDTO createUser(
            @Valid @RequestBody UserRequestDTO request,
            Authentication authentication) {

        CustomUserDetails currentUser = (CustomUserDetails) authentication.getPrincipal();

        log.info("User {} creating new user {}", currentUser.getUserId(), request.getEmail());

        // Role-based validation
        var requestedRole = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new RuntimeException("Role not found with id: " + request.getRoleId()));
        
        String requestedRoleName = requestedRole.getRoleName();
        String currentUserRole = currentUser.getRole();
        
        // CLIENT can only create STAFF users
        if ("CLIENT".equals(currentUserRole)) {
            if (!"STAFF".equals(requestedRoleName)) {
                throw new RuntimeException("CLIENT users can only create STAFF users");
            }
        }
        
        // STAFF cannot create any users (should be blocked at frontend, but double-check here)
        if ("STAFF".equals(currentUserRole)) {
            throw new RuntimeException("STAFF users cannot create other users");
        }
        
        // CLIENT cannot create CLIENT or SUPER_ADMIN users
        if ("CLIENT".equals(currentUserRole) && 
            ("CLIENT".equals(requestedRoleName) || "SUPER_ADMIN".equals(requestedRoleName))) {
            throw new RuntimeException("CLIENT users cannot create CLIENT or SUPER_ADMIN users");
        }

        User user = userMapper.toEntity(request);
        
        // Set password (should be temporary, user should reset on first login)
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        } else {
            // Generate temporary password
            user.setPassword(passwordEncoder.encode("TempPass123!"));
        }
        
        // Set role
        user.setRole(requestedRole);
        
        // Check if user with same email already exists (case-insensitive)
        var existingOpt = userRepository.findByEmailIgnoreCase(request.getEmail());
        if (existingOpt.isPresent()) {
            User existing = existingOpt.get();
            if (!existing.getActive()) {
                // Reactivate the existing user
                existing.setActive(true);
                existing.setDeletedAt(null);
                existing.setDeletedBy(null);
                existing.setUpdatedAt(LocalDateTime.now());
                existing.setUpdatedBy(currentUser.getUserId());
                if (request.getPassword() != null && !request.getPassword().isEmpty()) {
                    existing.setPassword(passwordEncoder.encode(request.getPassword()));
                }
                User reactivated = userRepository.save(existing);
                return userMapper.toDto(reactivated);
            } else {
                throw new RuntimeException("A user with this email already exists");
            }
        }

        // Set company
        if ("SUPER_ADMIN".equals(currentUserRole) && 
            ("CLIENT".equals(requestedRoleName) || "STAFF".equals(requestedRoleName))) {
            // SUPER_ADMIN creating CLIENT or STAFF - use companyId from request
            if (request.getCompanyId() != null) {
                var company = companyRepository.findById(request.getCompanyId())
                        .orElseThrow(() -> new RuntimeException("Company not found with id: " + request.getCompanyId()));
                user.setCompany(company);
            } else {
                throw new RuntimeException("Company ID is required when creating CLIENT or STAFF users");
            }
        } else if (currentUser.getCompanyId() != null) {
            // CLIENT creating STAFF - inherit company from current user
            var company = companyRepository.findById(currentUser.getCompanyId())
                    .orElseThrow(() -> new RuntimeException("Company not found"));
            user.setCompany(company);
        }
        
        // Set creator
        user.setCreatedBy(currentUser.getUserId());
        user.setCreatedAt(LocalDateTime.now());
        user.setActive(request.getActive() != null ? request.getActive() : true);

        User savedUser = userRepository.save(user);

        return userMapper.toDto(savedUser);
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<UserDTO> getUsers(Authentication authentication) {

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();

        log.info("Fetching users for user {}, role {}", user.getUserId(), user.getRole());

        List<User> users;

        if ("SUPER_ADMIN".equals(user.getRole())) {
            users = userRepository.findAll();
        } else if ("CLIENT".equals(user.getRole()) && user.getCompanyId() != null) {
            users = userService.getUsersByCompany(user.getCompanyId());
        } else {
            users = userRepository.findByCreatedBy(user.getUserId());
        }

        return users.stream()
                .map(userMapper::toDto)
                .toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public UserDTO getUser(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(userMapper::toDto)
                .orElseThrow(() -> new com.satyam.quotation.exception.ResourceNotFoundException(
                        "User not found with id: " + id));
    }

    @PutMapping("/{id}")
    public UserDTO updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserRequestDTO request,
            Authentication authentication) {

        CustomUserDetails currentUser = (CustomUserDetails) authentication.getPrincipal();

        log.info("User {} updating user {}", currentUser.getUserId(), id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new com.satyam.quotation.exception.ResourceNotFoundException(
                        "User not found with id: " + id));

        userMapper.updateEntity(request, user);
        
        // Update role if changed
        if (request.getRoleId() != null && !request.getRoleId().equals(user.getRole().getId())) {
            var role = roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new RuntimeException("Role not found with id: " + request.getRoleId()));
            user.setRole(role);
        }
        
        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy(currentUser.getUserId());

        User updatedUser = userRepository.save(user);

        return userMapper.toDto(updatedUser);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(
            @PathVariable Long id,
            Authentication authentication) {

        CustomUserDetails currentUser = (CustomUserDetails) authentication.getPrincipal();

        log.info("User {} deleting user {}", currentUser.getUserId(), id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new com.satyam.quotation.exception.ResourceNotFoundException(
                        "User not found with id: " + id));

        user.setActive(false);
        user.setDeletedAt(LocalDateTime.now());
        user.setDeletedBy(currentUser.getUserId());

        userRepository.save(user);
        
        log.info("User {} successfully deleted (soft delete)", id);
    }
}
