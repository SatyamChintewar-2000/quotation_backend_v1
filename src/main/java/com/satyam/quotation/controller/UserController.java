package com.satyam.quotation.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
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
    private final CacheManager cacheManager;

    public UserController(UserService userService,
                         UserMapper userMapper,
                         UserRepository userRepository,
                         RoleRepository roleRepository,
                         CompanyRepository companyRepository,
                         PasswordEncoder passwordEncoder,
                         CacheManager cacheManager) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.companyRepository = companyRepository;
        this.passwordEncoder = passwordEncoder;
        this.cacheManager = cacheManager;
    }

    /** Evict only the affected company's user cache — other companies unaffected. */
    private void evictUserCache(Long companyId) {
        var cache = cacheManager.getCache("users");
        if (cache != null) {
            if (companyId != null) {
                cache.evict("company:" + companyId);
                cache.evict("staff:"   + companyId);
            }
            cache.evict("all");
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDTO createUser(
            @Valid @RequestBody UserRequestDTO request,
            Authentication authentication) {

        CustomUserDetails currentUser = (CustomUserDetails) authentication.getPrincipal();
        log.info("User {} creating new user {}", currentUser.getUserId(), request.getEmail());

        var requestedRole = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new RuntimeException("Role not found with id: " + request.getRoleId()));

        String requestedRoleName = requestedRole.getRoleName();
        String currentUserRole   = currentUser.getRole();

        if ("CLIENT".equals(currentUserRole) && !"STAFF".equals(requestedRoleName)) {
            throw new RuntimeException("CLIENT users can only create STAFF users");
        }
        if ("STAFF".equals(currentUserRole)) {
            throw new RuntimeException("STAFF users cannot create other users");
        }
        if ("CLIENT".equals(currentUserRole) &&
                ("CLIENT".equals(requestedRoleName) || "SUPER_ADMIN".equals(requestedRoleName))) {
            throw new RuntimeException("CLIENT users cannot create CLIENT or SUPER_ADMIN users");
        }

        User user = userMapper.toEntity(request);

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        } else {
            user.setPassword(passwordEncoder.encode("TempPass123!"));
        }
        user.setRole(requestedRole);

        // Reactivate if email already exists (soft-deleted)
        var existingOpt = userRepository.findByEmailIgnoreCase(request.getEmail());
        if (existingOpt.isPresent()) {
            User existing = existingOpt.get();
            if (!existing.getActive()) {
                existing.setActive(true);
                existing.setDeletedAt(null);
                existing.setDeletedBy(null);
                existing.setUpdatedAt(LocalDateTime.now());
                existing.setUpdatedBy(currentUser.getUserId());
                if (request.getPassword() != null && !request.getPassword().isEmpty()) {
                    existing.setPassword(passwordEncoder.encode(request.getPassword()));
                }
                User reactivated = userRepository.save(existing);
                Long cid = reactivated.getCompany() != null ? reactivated.getCompany().getId() : null;
                evictUserCache(cid);
                return userMapper.toDto(reactivated);
            } else {
                throw new RuntimeException("A user with this email already exists");
            }
        }

        // Assign company
        if ("SUPER_ADMIN".equals(currentUserRole)) {
            if ("CLIENT".equals(requestedRoleName) || "STAFF".equals(requestedRoleName)) {
                if (request.getCompanyId() != null) {
                    user.setCompany(companyRepository.findById(request.getCompanyId())
                            .orElseThrow(() -> new RuntimeException("Company not found with id: " + request.getCompanyId())));
                } else {
                    throw new RuntimeException("Company ID is required when creating CLIENT or STAFF users");
                }
            }
        } else if ("CLIENT".equals(currentUserRole)) {
            if (currentUser.getCompanyId() == null) {
                throw new RuntimeException("Cannot create user: Current user has no company assigned.");
            }
            long activeUsersInCompany = userRepository.countByCompanyIdAndActiveTrue(currentUser.getCompanyId());
            if (activeUsersInCompany >= 5) {
                throw new RuntimeException("USER_LIMIT_REACHED: Your plan allows a maximum of 5 users.");
            }
            var company = companyRepository.findById(currentUser.getCompanyId())
                    .orElseThrow(() -> new RuntimeException("Company not found with id: " + currentUser.getCompanyId()));
            user.setCompany(company);
            log.info("CLIENT user {} creating STAFF user with company ID: {}", currentUser.getUserId(), company.getId());
        }

        user.setCreatedBy(currentUser.getUserId());
        user.setCreatedAt(LocalDateTime.now());
        user.setActive(request.getActive() != null ? request.getActive() : true);

        User savedUser = userRepository.save(user);
        Long companyId = savedUser.getCompany() != null ? savedUser.getCompany().getId() : null;
        evictUserCache(companyId);

        return userMapper.toDto(savedUser);
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<UserDTO> getUsers(Authentication authentication) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        log.info("Fetching users for user {}, role {}", user.getUserId(), user.getRole());

        List<User> users;
        if ("SUPER_ADMIN".equals(user.getRole())) {
            users = userService.getAllUsers();
        } else if ("CLIENT".equals(user.getRole())) {
            users = userService.getUsersByCompany(user.getCompanyId());
        } else {
            users = userRepository.findByCreatedBy(user.getUserId());
        }

        return users.stream().map(userMapper::toDto).toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public UserDTO getUser(@PathVariable("id") Long id) {
        return userService.getUserById(id)
                .map(userMapper::toDto)
                .orElseThrow(() -> new com.satyam.quotation.exception.ResourceNotFoundException(
                        "User not found with id: " + id));
    }

    @PutMapping("/{id}")
    public UserDTO updateUser(
            @PathVariable("id") Long id,
            @Valid @RequestBody UserRequestDTO request,
            Authentication authentication) {

        CustomUserDetails currentUser = (CustomUserDetails) authentication.getPrincipal();
        log.info("User {} updating user {}", currentUser.getUserId(), id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new com.satyam.quotation.exception.ResourceNotFoundException(
                        "User not found with id: " + id));

        userMapper.updateEntity(request, user);

        if (request.getRoleId() != null && !request.getRoleId().equals(user.getRole().getId())) {
            var role = roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new RuntimeException("Role not found with id: " + request.getRoleId()));
            user.setRole(role);
        }

        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy(currentUser.getUserId());

        User updatedUser = userRepository.save(user);
        Long companyId = updatedUser.getCompany() != null ? updatedUser.getCompany().getId() : null;
        evictUserCache(companyId);

        return userMapper.toDto(updatedUser);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(
            @PathVariable("id") Long id,
            Authentication authentication) {

        CustomUserDetails currentUser = (CustomUserDetails) authentication.getPrincipal();
        log.info("User {} deleting user {}", currentUser.getUserId(), id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new com.satyam.quotation.exception.ResourceNotFoundException(
                        "User not found with id: " + id));

        Long companyId = user.getCompany() != null ? user.getCompany().getId() : null;

        user.setActive(false);
        user.setDeletedAt(LocalDateTime.now());
        user.setDeletedBy(currentUser.getUserId());
        userRepository.save(user);

        evictUserCache(companyId);
        log.info("User {} successfully deleted (soft delete)", id);
    }

    @PutMapping("/{id}/reset-password")
    public java.util.Map<String, String> resetUserPassword(
            @PathVariable("id") Long id,
            @RequestBody java.util.Map<String, String> body,
            Authentication authentication) {

        CustomUserDetails currentUser = (CustomUserDetails) authentication.getPrincipal();
        String newPassword = body.get("newPassword");

        if (newPassword == null || newPassword.length() < 6) {
            throw new com.satyam.quotation.exception.BadRequestException("Password must be at least 6 characters");
        }

        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new com.satyam.quotation.exception.ResourceNotFoundException("User not found"));

        String currentRole = currentUser.getRole();
        if ("CLIENT".equals(currentRole)) {
            String targetRole = targetUser.getRole() != null ? targetUser.getRole().getRoleName() : "";
            if (!"STAFF".equals(targetRole)) {
                throw new RuntimeException("You can only reset passwords for STAFF users");
            }
            Long targetCompanyId = targetUser.getCompany() != null ? targetUser.getCompany().getId() : null;
            if (!currentUser.getCompanyId().equals(targetCompanyId)) {
                throw new RuntimeException("You can only reset passwords for users in your company");
            }
        } else if (!"SUPER_ADMIN".equals(currentRole)) {
            throw new RuntimeException("You are not authorised to reset other users' passwords");
        }

        targetUser.setPassword(passwordEncoder.encode(newPassword));
        targetUser.setUpdatedAt(LocalDateTime.now());
        targetUser.setUpdatedBy(currentUser.getUserId());
        userRepository.save(targetUser);

        log.info("User {} reset password for user {}", currentUser.getUserId(), id);
        return java.util.Map.of("message", "Password reset successfully");
    }
}
