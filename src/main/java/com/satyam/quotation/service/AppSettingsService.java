package com.satyam.quotation.service;

import java.util.Map;

public interface AppSettingsService {
    Map<String, String> getAllSettings();
    String getSetting(String key, String defaultValue);
    boolean getBooleanSetting(String key, boolean defaultValue);
    void updateSettings(Map<String, String> settings, Long userId);
}
