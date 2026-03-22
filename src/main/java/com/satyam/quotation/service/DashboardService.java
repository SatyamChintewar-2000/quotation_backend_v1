package com.satyam.quotation.service;

import java.util.Map;

public interface DashboardService {
    Map<String, Object> getDashboardData(Long userId, String role, Long companyId);
}
