package com.satyam.quotation.service.impl;

import com.satyam.quotation.model.AppSettings;
import com.satyam.quotation.repository.AppSettingsRepository;
import com.satyam.quotation.service.AppSettingsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AppSettingsServiceImpl implements AppSettingsService {

    private final AppSettingsRepository repository;

    public AppSettingsServiceImpl(AppSettingsRepository repository) {
        this.repository = repository;
    }

    @Override
    public Map<String, String> getAllSettings() {
        List<AppSettings> all = repository.findAll();
        return all.stream().collect(Collectors.toMap(AppSettings::getSettingKey, AppSettings::getSettingValue));
    }

    @Override
    public String getSetting(String key, String defaultValue) {
        return repository.findBySettingKey(key)
                .map(AppSettings::getSettingValue)
                .orElse(defaultValue);
    }

    @Override
    public boolean getBooleanSetting(String key, boolean defaultValue) {
        String value = getSetting(key, String.valueOf(defaultValue));
        return "true".equalsIgnoreCase(value);
    }

    @Override
    @Transactional
    public void updateSettings(Map<String, String> settings, Long userId) {
        settings.forEach((key, value) -> {
            AppSettings setting = repository.findBySettingKey(key)
                    .orElse(AppSettings.builder().settingKey(key).build());
            setting.setSettingValue(value);
            setting.setUpdatedAt(LocalDateTime.now());
            setting.setUpdatedBy(userId);
            repository.save(setting);
        });
    }
}
