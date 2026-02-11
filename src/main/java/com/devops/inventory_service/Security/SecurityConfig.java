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
                // 1. Tắt CSRF
                .csrf(csrf -> csrf.disable())

                // 2. Chế độ Stateless
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 3. 🛡️ QUẢN LÝ HTTPS
                .requiresChannel(channel -> channel
                        .requestMatchers("/actuator/**", "/health/**").requiresInsecure()
                        .anyRequest().requiresSecure()
                )

                // 4. 🛡️ SECURITY HEADERS
                .headers(headers -> headers
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)
                                .preload(true)
                        )
                        .frameOptions(frame -> frame.deny())
                        .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; script-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none';")
                        )
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .contentTypeOptions(Customizer.withDefaults())
                )

                // 5. Phân quyền truy cập (AUTHORIZATION)
                .authorizeHttpRequests(auth -> auth
                        // Public Endpoints
                        .requestMatchers("/actuator/**", "/health/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/inventory/version").permitAll()

                        // === [START] ADDED NEW RULES FOR HARDENING ===

                        // Feature 8: Export Báo Cáo -> Chỉ ADMIN (Resource Killer Prevention)
                        .requestMatchers("/api/inventory/export").hasRole("ADMIN")

                        // Feature 7: Upload Ảnh -> Chỉ ADMIN (Malware Prevention)
                        // Chỉ chặn method POST, còn GET ảnh (nếu có) thì có thể cho User xem
                        .requestMatchers(HttpMethod.POST, "/api/inventory/{id}/image").hasRole("ADMIN")

                        // Feature 6: Orders -> Phải Login (IDOR Prevention logic sẽ nằm ở Service layer)
                        .requestMatchers("/api/orders/**").authenticated()

                        // === [END] ADDED NEW RULES ===

                        // Default: Các API còn lại bắt buộc phải Login
                        .anyRequest().authenticated()
                );

        // 6. Config Authentication Provider & Filter
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