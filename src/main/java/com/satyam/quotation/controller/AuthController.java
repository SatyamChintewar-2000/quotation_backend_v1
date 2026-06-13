package com.satyam.quotation.controller;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.satyam.quotation.model.RefreshToken;
import com.satyam.quotation.repository.UserRepository;
import com.satyam.quotation.security.CustomUserDetails;
import com.satyam.quotation.security.JwtService;
import com.satyam.quotation.service.AppSettingsService;
import com.satyam.quotation.service.EmailService;
import com.satyam.quotation.service.RefreshTokenService;
import com.satyam.quotation.service.SmsService;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{6,15}$");

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SmsService smsService;
    private final AppSettingsService appSettingsService;

    public AuthController(AuthenticationManager authenticationManager,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            SmsService smsService,
            AppSettingsService appSettingsService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.smsService = smsService;
        this.appSettingsService = appSettingsService;
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
                    new UsernamePasswordAuthenticationToken(email, password));

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            var user = userDetails.getUser();
            return ResponseEntity.ok(buildAuthPayload(user));
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
                    companyId);

            return ResponseEntity.ok(Map.of(
                    "accessToken", newAccessToken));
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

    @PostMapping("/request-login-otp")
    public ResponseEntity<?> requestLoginOtp(@RequestBody Map<String, String> request) {
        if (!appSettingsService.getBooleanSetting("mobile_otp_login_enabled", false)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Mobile OTP login is currently disabled"));
        }

        String phone = request.get("phone");
        if (phone != null) {
            phone = phone.trim();
        }
        if (phone == null || phone.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Phone number is required"));
        }
        String normalizedPhone = normalizePhoneDigits(phone);
        if (!PHONE_PATTERN.matcher(normalizedPhone).matches()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Enter a valid phone number"));
        }
        var userOpt = userRepository.findByPhone(normalizedPhone);
        if (userOpt.isEmpty() && normalizedPhone.startsWith("0")) {
            userOpt = userRepository.findByPhone(normalizedPhone.substring(1));
        }
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Phone number is not registered"));
        }
        var user = userOpt.get();
        String otpCode = String.format("%06d", secureRandom.nextInt(900000) + 100000);
        user.setLoginOtp(otpCode);
        user.setLoginOtpExpiry(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        String sendTo = resolveSendPhone(user.getCountryCode(), user.getPhone());
        try {
            smsService.sendSms(sendTo, String.format("Your login OTP is %s. It expires in 10 minutes.", otpCode));
        } catch (Exception e) {
            log.error("Failed to send login OTP to {}", sendTo, e);
            String causeMessage = e.getMessage();
            String errorMessage = causeMessage != null && !causeMessage.isBlank()
                    ? causeMessage
                    : "Unable to send OTP at this time";
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", errorMessage));
        }

        if (smsService.isMockProvider()) {
            Map<String, Object> body = new java.util.HashMap<>();
            body.put("message", "OTP sent to your mobile number");
            body.put("otp", otpCode);
            return ResponseEntity.ok(body);
        }

        return ResponseEntity.ok(Map.of("message", "OTP sent to your mobile number"));
    }

    @PostMapping("/verify-login-otp")
    public ResponseEntity<?> verifyLoginOtp(@RequestBody Map<String, String> request) {
        if (!appSettingsService.getBooleanSetting("mobile_otp_login_enabled", false)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Mobile OTP login is currently disabled"));
        }

        String phone = request.get("phone");
        String otp = request.get("otp");
        if (phone != null) {
            phone = phone.trim();
        }
        if (phone == null || phone.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Phone number is required"));
        }
        if (otp == null || otp.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "OTP is required"));
        }
        String normalizedPhone = normalizePhoneDigits(phone);
        if (!PHONE_PATTERN.matcher(normalizedPhone).matches()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Enter a valid phone number"));
        }
        var userOpt = userRepository.findByPhone(normalizedPhone);
        if (userOpt.isEmpty() && normalizedPhone.startsWith("0")) {
            userOpt = userRepository.findByPhone(normalizedPhone.substring(1));
        }
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Phone number is not registered"));
        }
        var user = userOpt.get();
        if (user.getLoginOtp() == null || !user.getLoginOtp().equals(otp)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid OTP"));
        }
        if (user.getLoginOtpExpiry() == null || user.getLoginOtpExpiry().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "OTP has expired"));
        }
        user.setLoginOtp(null);
        user.setLoginOtpExpiry(null);
        userRepository.save(user);
        return ResponseEntity.ok(buildAuthPayload(user));
    }

    @GetMapping("/login-features")
    public ResponseEntity<?> getLoginFeatures() {
        boolean mobileOtpEnabled = appSettingsService.getBooleanSetting("mobile_otp_login_enabled", false);
        return ResponseEntity.ok(Map.of("mobileOtpEnabled", mobileOtpEnabled));
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
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid or expired reset token"));
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

    private static String normalizePhoneDigits(String phone) {
        return phone == null ? "" : phone.replaceAll("[^0-9]", "");
    }

    private static String resolveSendPhone(String countryCode, String phone) {
        String digits = normalizePhoneDigits(phone);
        if (digits.startsWith("0")) {
            digits = digits.substring(1);
        }
        if (countryCode != null && !countryCode.isBlank()) {
            if (!countryCode.startsWith("+")) {
                countryCode = "+" + countryCode;
            }
            return countryCode + digits;
        }
        return "+" + digits;
    }

    private Map<String, Object> buildAuthPayload(com.satyam.quotation.model.User user) {
        String role = user.getRole() != null ? user.getRole().getRoleName() : "USER";
        String accessToken = jwtService.generateAccessToken(
                user.getEmail(),
                role,
                user.getId(),
                user.getCompany() != null ? user.getCompany().getId() : null);

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getEmail());
        String formattedRole = role.toLowerCase().replace("_", "");

        return Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken.getToken(),
                "user", Map.of(
                        "id", user.getId().toString(),
                        "email", user.getEmail(),
                        "name", user.getName(),
                        "role", formattedRole,
                        "avatar", user.getAvatar() != null ? user.getAvatar() : "",
                        "companyId", user.getCompany() != null ? user.getCompany().getId() : 0L));
    }
}
