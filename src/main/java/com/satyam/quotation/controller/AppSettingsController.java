package com.satyam.quotation.controller;

import com.satyam.quotation.security.CustomUserDetails;
import com.satyam.quotation.service.AppSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class AppSettingsController {

    private final AppSettingsService settingsService;

    public AppSettingsController(AppSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public ResponseEntity<Map<String, String>> getSettings() {
        return ResponseEntity.ok(settingsService.getAllSettings());
    }

    @PutMapping
    public ResponseEntity<?> updateSettings(
            @RequestBody Map<String, String> settings,
            Authentication authentication) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        settingsService.updateSettings(settings, user.getUserId());
        return ResponseEntity.ok(Map.of("success", true, "message", "Settings updated successfully"));
    }
}
