package com.devops.inventory_service.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority; // Import cái này
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    // private final UserDetailsServiceImpl userDetailsService; <--- XOÁ DÒNG NÀY ĐI BRO! KHÔNG CẦN NỮA

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String token = parseJwt(request); // Hàm tách chuỗi "Bearer " bro tự viết hoặc copy logic cũ xuống

            if (token != null && jwtUtils.validateJwtToken(token)) {

                // 1. Lấy username từ token
                String username = jwtUtils.getUsernameFromJwtToken(token);

                // 2. Lấy List Role trực tiếp từ Token (Siêu nhanh, ko query DB)
                List<String> roles = jwtUtils.getRolesFromJwtToken(token);

                // 3. Convert String Role sang GrantedAuthority của Spring Security
                // Vì trong DB bro đã lưu sẵn "ROLE_ADMIN" rồi nên cứ thế map vào thôi
                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                // 4. Tạo đối tượng Authentication
                // Tham số thứ 2 là credentials, để null vì mình đã tin tưởng token rồi
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 5. Set vào Context
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (Exception e) {
            // Log lỗi nếu cần: logger.error("Cannot set user authentication: {}", e);
        }

        filterChain.doFilter(request, response);
    }

    // Helper function lấy token từ header
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}