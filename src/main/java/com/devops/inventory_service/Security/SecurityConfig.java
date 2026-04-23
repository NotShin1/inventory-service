package com.devops.inventory_service.Security;

import com.devops.inventory_service.Security.JwtAuthFilter;
import com.devops.inventory_service.Security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
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
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    // ========================================================================
    // 🔥 CHAIN 1: DÀNH RIÊNG CHO ACTUATOR & PROMETHEUS (Dùng Basic Auth)
    // ========================================================================
    @Bean
    @Order(1) // Bắt buộc phải có Order(1) để nó chạy trước tiên
    public SecurityFilterChain actuatorFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/actuator/**") // Mấu chốt: Chain này CHỈ hứng các request bắt đầu bằng /actuator
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requiresChannel(channel -> channel.anyRequest().requiresInsecure()) // Prometheus gọi nội bộ qua HTTP
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**").permitAll() // Docker Healthcheck không cần pass
                        .anyRequest().hasRole("ADMIN") // Prometheus lấy metrics phải có Role ADMIN
                )
                // 👉 BẬT BASIC AUTH ở đây (Chỉ áp dụng cho /actuator)
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    // ========================================================================
    // 🛡️ CHAIN 2: DÀNH CHO API NGHIỆP VỤ & SWAGGER (Dùng JWT, KHÔNG Basic Auth)
    // ========================================================================
    @Bean
    @Order(2)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // BẢO MẬT KÊNH TRUYỀN (Yêu cầu HTTPS)
                .requiresChannel(channel -> channel.anyRequest().requiresSecure())

                .headers(headers -> headers
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000).preload(true))
                        .frameOptions(frame -> frame.deny())
                        .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; script-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none';"))
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .contentTypeOptions(Customizer.withDefaults())
                )

                // PHÂN QUYỀN TRUY CẬP API
                .authorizeHttpRequests(auth -> auth
                        // Mở toang cửa cho Swagger (Thêm /webjars/** để load UI không bị lỗi 401)
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/webjars/**").permitAll()

                        // Các API Public khác
                        .requestMatchers("/api/auth/**", "/api/inventory/version").permitAll()

                        // Các API yêu cầu Role ADMIN
                        .requestMatchers("/api/inventory/export").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/inventory/{id}/image").hasRole("ADMIN")

                        // Mặc định: Phải có Token hợp lệ
                        .anyRequest().authenticated()
                );

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