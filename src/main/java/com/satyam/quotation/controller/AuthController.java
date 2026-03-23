package com.satyam.quotation.controller;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.satyam.quotation.model.RefreshToken;
import com.satyam.quotation.repository.UserRepository;
import com.satyam.quotation.security.CustomUserDetails;
import com.satyam.quotation.security.JwtService;
import com.satyam.quotation.service.RefreshTokenService;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          RefreshTokenService refreshTokenService,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String password = request.get("password");

            log.debug("Login attempt - email: '{}', password length: {}, password bytes: {}",
                    email,
                    password != null ? password.length() : "null",
                    password != null ? java.util.Arrays.toString(password.getBytes()) : "null");

            // Direct BCrypt check for debugging
            var userOpt = userRepository.findByEmailIgnoreCase(email != null ? email.toLowerCase() : "");
            if (userOpt.isPresent()) {
                boolean matches = passwordEncoder.matches(password, userOpt.get().getPassword());
                log.debug("Direct BCrypt check - stored hash: '{}', matches: {}",
                        userOpt.get().getPassword(), matches);
            }

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            var user = userDetails.getUser();

            // Generate access token (15 minutes)
            String accessToken = jwtService.generateAccessToken(
                    userDetails.getUsername(),
                    userDetails.getRole(),
                    userDetails.getUserId(),
                    userDetails.getCompanyId()
            );

            // Generate and save refresh token (7 days)
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getUsername());

            // Format role for frontend (lowercase, no underscores)
            String formattedRole = userDetails.getRole().toLowerCase().replace("_", "");

            return ResponseEntity.ok(Map.of(
                    "accessToken", accessToken,
                    "refreshToken", refreshToken.getToken(),
                    "user", Map.of(
                            "id", userDetails.getUserId().toString(),
                            "email", user.getEmail(),
                            "name", user.getName(),
                            "role", formattedRole,
                            "avatar", user.getAvatar() != null ? user.getAvatar() : "",
                            "companyId", userDetails.getCompanyId() != null ? userDetails.getCompanyId() : 0L
                    )
            ));
        } catch (Exception e) {
            log.error("Login failed for email: {} | Exception type: {} | Message: {}",
                    request.get("email"),
                    e.getClass().getName(),
                    e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");
            
            if (refreshToken == null || refreshToken.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Refresh token is required"));
            }

            // Verify refresh token
            RefreshToken validRefreshToken = refreshTokenService.verifyRefreshToken(refreshToken);
            
            // Get user details
            var user = validRefreshToken.getUser();
            String role = user.getRole().getRoleName();
            Long companyId = user.getCompany() != null ? user.getCompany().getId() : null;

            // Generate new access token
            String newAccessToken = jwtService.generateAccessToken(
                    user.getEmail(),
                    role,
                    user.getId(),
                    companyId
            );

            return ResponseEntity.ok(Map.of(
                    "accessToken", newAccessToken
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");
            
            if (refreshToken != null && !refreshToken.isEmpty()) {
                refreshTokenService.deleteRefreshToken(refreshToken);
            }

            return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }
        var userOpt = userRepository.findByEmailIgnoreCase(email.toLowerCase());
        if (userOpt.isEmpty()) {
            // Return success even if not found to prevent email enumeration
            return ResponseEntity.ok(Map.of("message", "If that email exists, a reset token has been sent"));
        }
        var user = userOpt.get();
        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);
        // In production, send this token via email. For now, return it directly.
        return ResponseEntity.ok(Map.of(
            "message", "Password reset token generated",
            "resetToken", token
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("newPassword");
        if (token == null || newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "Valid token and password (min 6 chars) required"));
        }
        var userOpt = userRepository.findByResetToken(token);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid or expired reset token"));
        }
        var user = userOpt.get();
        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Reset token has expired"));
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }
}
