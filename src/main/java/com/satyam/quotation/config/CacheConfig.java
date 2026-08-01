package com.satyam.quotation.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * In-memory cache configuration using Caffeine.
 *
 * Cache strategy per type:
 *  - quotations   : TTL 2 min  — refreshed after every create/update/delete
 *  - customers    : TTL 5 min
 *  - products     : TTL 5 min
 *  - users        : TTL 5 min
 *  - enquiries    : TTL 2 min
 *  - invoices     : TTL 2 min
 *  - dashboard    : TTL 1 min  — stats page, short TTL for freshness
 *  - reports      : TTL 2 min
 *
 * Cache entries are evicted automatically on write operations via @CacheEvict.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(
                "quotations",
                "customers",
                "products",
                "users",
                "enquiries",
                "invoices",
                "dashboard",
                "reports"
        );
        // Default spec: max 500 entries, expire 2 min after last write
        manager.setCaffeine(
                Caffeine.newBuilder()
                        .maximumSize(500)
                        .expireAfterWrite(2, TimeUnit.MINUTES)
        );
        return manager;
    }
}
