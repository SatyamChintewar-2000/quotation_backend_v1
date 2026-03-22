package com.satyam.quotation.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://localhost:5173",  // Vite default port
                        "http://localhost:3000",  // Alternative React port
                        "http://localhost:4173",
                        "http://localhost:8082",
                        "http://localhost:8081",
                        "http://localhost:8080",
                        "http://quotation-app-frontend-satyam.s3-website.eu-north-1.amazonaws.com"// Vite preview port
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
