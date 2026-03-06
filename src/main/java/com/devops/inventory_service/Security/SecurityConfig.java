package com.devops.inventory_service.Security;

import com.devops.inventory_service.Security.JwtAuthFilter;
import com.devops.inventory_service.Security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. Tắt CSRF (Do dùng JWT/Stateless API)
                .csrf(csrf -> csrf.disable())

                // 2. Chế độ Stateless (Không lưu session server-side)
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 3. 🛡️ QUẢN LÝ HTTPS & KÊNH TRUYỀN
                .requiresChannel(channel -> channel
                        // Cho phép HTTP đối với Actuator/Health (để Docker check nội bộ)
                        .requestMatchers("/actuator/**", "/health/**").requiresInsecure()
                        // Các API nghiệp vụ bắt buộc HTTPS
                        .anyRequest().requiresSecure()
                )

                // 4. 🛡️ SECURITY HEADERS (Cấu hình chuẩn Hardening)
                .headers(headers -> headers
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)
                                .preload(true)
                        )
                        .frameOptions(frame -> frame.deny()) // Chống Clickjacking
                        .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; script-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none';")
                        )
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .contentTypeOptions(Customizer.withDefaults())
                )

                // 5. 🚦 PHÂN QUYỀN TRUY CẬP (AUTHORIZATION)
                .authorizeHttpRequests(auth -> auth
                        // --- KHU VỰC PUBLIC (Ai cũng vào được) ---
                        // 🔥 QUAN TRỌNG: Mở cửa cho Docker Healthcheck chọc vào
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Login/Register
                        .requestMatchers("/api/auth/**").permitAll()
                        // API check version
                        .requestMatchers("/api/inventory/version").permitAll()

                        // --- KHU VỰC ADMIN / MONITORING (Cần bảo mật cao) ---

                        // 🔐 Prometheus Metrics & Actuator khác -> Cần Role ADMIN
                        .requestMatchers("/actuator/**").hasRole("ADMIN")

                        // Feature 8: Export Báo Cáo -> Chỉ ADMIN (Chống Resource Exhaustion)
                        .requestMatchers("/api/inventory/export").hasRole("ADMIN")

                        // Feature 7: Upload Ảnh -> Chỉ ADMIN (Chống Malware Upload)
                        .requestMatchers(HttpMethod.POST, "/api/inventory/{id}/image").hasRole("ADMIN")

                        // --- KHU VỰC USER / AUTHENTICATED ---

                        // Feature 6: Xem đơn hàng (IDOR logic nằm ở Service) -> Phải Login
                        .requestMatchers("/api/orders/**").authenticated()

                        // Default: Các API còn lại bắt buộc phải Login
                        .anyRequest().authenticated()
                )

                // 6. 🛡️ CẤU HÌNH AUTHENTICATION

                // 🔥 BẬT BASIC AUTH: Để Prometheus Server có thể login (bằng user/pass) để lấy metrics
                // Nếu không có dòng này, Prometheus sẽ bị chặn (401) vì nó không biết dùng JWT
                .httpBasic(Customizer.withDefaults());

        // 7. Thêm JWT Filter cho User người dùng thật
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