package com.devops.inventory_service.Security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {

    // Logger để ghi log lỗi (thay vì System.err) - Chuẩn DevOps hơn
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    // Inject giá trị từ application.properties
    // Spring sẽ tự tìm key 'app.jwtSecret' đã map với biến môi trường JWT_SECRET
    @Value("${app.jwtSecret}")
    private String jwtSecret;

    // Inject thời gian hết hạn (tính bằng ms)
    @Value("${app.jwtExpirationMs}")
    private int jwtExpirationMs;

    /**
     * Chuyển chuỗi Secret (String) thành Key Object chuẩn HMAC-SHA.
     * Lưu ý: Chuỗi trong .env phải đủ dài (ít nhất 32 ký tự / 256 bit).
     */
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * TẠO TOKEN (Generate JWT)
     * @param username Tên đăng nhập của user
     * @return Chuỗi JWT Token
     */
    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username) // Payload: Chứa username
                .setIssuedAt(new Date()) // Payload: Thời gian tạo
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs)) // Payload: Thời gian hết hạn
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // Header + Signature: Ký bằng thuật toán HS256
                .compact();
    }

    /**
     * GIẢI MÃ TOKEN (Get Username)
     * @param token Chuỗi JWT cần giải mã
     * @return Username nằm trong token
     */
    public String getUsernameFromJwtToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    /**
     * KIỂM TRA TÍNH HỢP LỆ (Validate Token)
     * Check xem token có bị sửa đổi, hết hạn, hoặc sai chữ ký không.
     */
    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(authToken);
            return true;
        } catch (SignatureException e) {
            logger.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
}