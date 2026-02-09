package com.devops.inventory_service.Security; // Check lại package của bro nhé

import com.devops.inventory_service.Security.JwtAuthFilter; // Check import filter
import com.devops.inventory_service.Security.UserDetailsServiceImpl; // Check import service
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // Để xài được @PreAuthorize
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. Tắt CSRF (API stateless không cần)
                .csrf(csrf -> csrf.disable())

                // 2. Chế độ Stateless (Không lưu session)
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 3. 🛡️ ÉP BUỘC HTTPS (Cho Domain thật của bro)
                .requiresChannel(channel -> channel.anyRequest().requiresSecure())

                // 4. 🛡️ SECURITY HEADERS (Cấu hình "Hardened" chống tấn công)
                .headers(headers -> headers
                        // HSTS: Ép trình duyệt dùng HTTPS trong 1 năm
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)
                        )
                        // Chống Clickjacking
                        .frameOptions(frame -> frame.deny())
                        // Chống XSS (Browser block)
                        .xssProtection(xss -> xss.headerValue(org.springframework.security.web.header.writers.XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                        // Content Security Policy (CSP): Chỉ cho phép script từ chính domain mình
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; script-src 'self'; object-src 'none'; base-uri 'self';")
                        )
                        // Referrer Policy
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                )

                // 5. Phân quyền (Giữ nguyên logic cũ của bro)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**", "/health/**").permitAll() // Cho Monitoring
                        .requestMatchers("/api/auth/**", "/api/inventory/version").permitAll() // Public API
                        .anyRequest().authenticated() // Còn lại phải có Token
                );

        // 6. Config Authentication Provider & Filter cũ của bro
        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
}