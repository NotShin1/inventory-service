package com.devops.inventory_service.Service;

import com.devops.inventory_service.Model.RefreshToken;
import com.devops.inventory_service.Model.User;
import com.devops.inventory_service.Repository.RefreshTokenRepository;
import com.devops.inventory_service.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    @Value("${app.jwtRefreshExpirationMs}")
    private Long refreshTokenDurationMs;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    // --- HÀM TẠO TOKEN (Dùng cho cả Login và Refresh) ---
    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User Not Found"));

        // 1. DỌN DẸP: Xóa token cũ của user này (Fix lỗi Duplicate Key)
        refreshTokenRepository.deleteByUser(user);

        // 2. TẠO MỚI
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        refreshToken.setToken(UUID.randomUUID().toString());

        return refreshTokenRepository.save(refreshToken);
    }

    // Check hạn sử dụng
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token hết hạn. Login lại đi bro!");
        }
        return token;
    }

    // Xóa token theo chuỗi (Dùng khi logout hoặc rotation)
    @Transactional
    public void deleteByToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(refreshTokenRepository::delete);
    }
}