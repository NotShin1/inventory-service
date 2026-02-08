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

    // --- FIX LỖI DUPLICATE KEY (Logic: Update nếu tồn tại) ---
    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User Not Found"));

        // 1. Kiểm tra xem user này đã có token trong DB chưa?
        RefreshToken refreshToken = refreshTokenRepository.findByUser(user)
                .orElse(new RefreshToken()); // Nếu chưa có -> New object

        // 2. Cập nhật thông tin (Dù mới hay cũ cũng set lại giá trị mới)
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        refreshToken.setToken(UUID.randomUUID().toString()); // Token mới tinh

        // 3. Lưu xuống (Hibernate sẽ tự hiểu: Có ID -> Update, Chưa ID -> Insert)
        return refreshTokenRepository.save(refreshToken);
    }

    // --- XOAY VÒNG TOKEN (Hàm này vẫn giữ nguyên logic xóa) ---
    // Nhưng vì ở trên mình dùng logic Update, nên hàm deleteByToken này
    // chủ yếu để Hacker không dùng lại được token cũ
    @Transactional
    public void deleteByToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(refreshTokenRepository::delete);
    }

    // Check hạn
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token đã hết hạn. Vui lòng đăng nhập lại!");
        }
        return token;
    }
}