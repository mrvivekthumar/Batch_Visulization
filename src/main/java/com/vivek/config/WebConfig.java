package com.vivek.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web Configuration to register interceptors and other web-related settings
 * 
 * @author Vivek
 * @version 1.0.0
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private RateLimitingConfig rateLimitingConfig;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Add rate limiting to all API endpoints
        registry.addInterceptor(rateLimitingConfig)
                .addPathPatterns("/api/**")
                .order(1); // Execute rate limiting first
    }
}