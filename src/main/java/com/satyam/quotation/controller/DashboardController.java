package com.satyam.quotation.controller;

import com.satyam.quotation.security.CustomUserDetails;

import com.satyam.quotation.service.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public Map<String, Object> getDashboard(Authentication authentication) {

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();

        log.info("Dashboard request by userId={}, role={}",
                user.getUserId(), user.getRole());

        return dashboardService.getDashboardData(
                user.getUserId(),
                user.getRole(),
                user.getCompanyId()
        );
    }
}
