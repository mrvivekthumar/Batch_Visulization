package com.vivek.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Security configuration for Database Batch Performance Analyzer
 * 
 * Provides:
 * - Basic authentication for API endpoints
 * - Security headers for protection against common attacks
 * - CORS configuration
 * - Rate limiting considerations
 * - Secure session management
 * 
 * @author Vivek
 * @version 1.0.0
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.security.admin.username:admin}")
    private String adminUsername;

    @Value("${app.security.admin.password:}")
    private String adminPassword;

    @Value("${app.security.viewer.username:viewer}")
    private String viewerUsername;

    @Value("${app.security.viewer.password:}")
    private String viewerPassword;

    @Value("${app.security.enabled:true}")
    private boolean securityEnabled;

    /**
     * Configure HTTP Security
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        if (!securityEnabled) {
            // Development mode - disable security but add headers
            http.authorizeHttpRequests(authz -> authz.anyRequest().permitAll())
                    .csrf(csrf -> csrf.disable())
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .headers(headers -> configureSecurityHeaders(headers));
            return http.build();
        }

        http
                // CSRF Configuration
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/v1/performance/**")
                        .csrfTokenRepository(
                                org.springframework.security.web.csrf.CookieCsrfTokenRepository.withHttpOnlyFalse()))

                // Session Management
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .maximumSessions(10)
                        .maxSessionsPreventsLogin(false))

                // Authorization Rules
                .authorizeHttpRequests(authz -> authz
                        // Public endpoints
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/favicon.ico",
                                "/actuator/health",
                                "/actuator/info")
                        .permitAll()

                        // Admin-only endpoints
                        .requestMatchers(
                                "/api/v1/performance/initialize",
                                "/api/v1/performance/delete",
                                "/actuator/**")
                        .hasRole("ADMIN")

                        // Read-only endpoints for viewers and admins
                        .requestMatchers(
                                "/api/v1/performance/stats/**",
                                "/api/v1/performance/health")
                        .hasAnyRole("VIEWER", "ADMIN")

                        // All other requests need authentication
                        .anyRequest().authenticated())

                // HTTP Basic Authentication
                .httpBasic(basic -> basic
                        .realmName("Database Performance Analyzer"))

                // Security Headers
                .headers(headers -> configureSecurityHeaders(headers))

                // CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()));

        return http.build();
    }

    /**
     * Configure comprehensive security headers
     */
    private void configureSecurityHeaders(
            org.springframework.security.config.annotation.web.configurers.HeadersConfigurer<?> headers) {
        headers
                .frameOptions(frameOptions -> frameOptions.deny())
                .contentTypeOptions(contentTypeOptions -> {
                })
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                        .maxAgeInSeconds(31536000)
                        .includeSubDomains(true))
                .referrerPolicy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                .and()
                .addHeaderWriter((request, response) -> {
                    // Custom security headers
                    response.addHeader("X-Content-Type-Options", "nosniff");
                    response.addHeader("X-Frame-Options", "DENY");
                    response.addHeader("X-XSS-Protection", "1; mode=block");
                    response.addHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
                    response.addHeader("Pragma", "no-cache");
                    response.addHeader("Expires", "0");
                    response.addHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=(), payment=()");
                });
    }

    /**
     * CORS Configuration for controlled cross-origin requests
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allowed origins (restrictive for production)
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "https://localhost:*"));

        // Allowed methods
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "OPTIONS"));

        // Allowed headers
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Cache-Control"));

        // Expose headers
        configuration.setExposedHeaders(Arrays.asList(
                "X-Total-Count",
                "X-Rate-Limit-Remaining"));

        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    /**
     * User Details Service with role-based access
     */
    @Bean
    public UserDetailsService userDetailsService() {
        // Validate that passwords are provided
        if (securityEnabled && (adminPassword.isEmpty() || viewerPassword.isEmpty())) {
            throw new IllegalStateException(
                    "Security is enabled but passwords not provided. " +
                            "Set app.security.admin.password and app.security.viewer.password");
        }

        UserDetails admin = User.builder()
                .username(adminUsername)
                .password(passwordEncoder().encode(adminPassword.isEmpty() ? "admin123!" : adminPassword))
                .roles("ADMIN", "VIEWER")
                .build();

        UserDetails viewer = User.builder()
                .username(viewerUsername)
                .password(passwordEncoder().encode(viewerPassword.isEmpty() ? "viewer123!" : viewerPassword))
                .roles("VIEWER")
                .build();

        return new InMemoryUserDetailsManager(admin, viewer);
    }

    /**
     * Password encoder using BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // Strong encryption rounds
    }
}