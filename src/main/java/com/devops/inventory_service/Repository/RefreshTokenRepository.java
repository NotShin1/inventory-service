package com.devops.inventory_service.Repository;

import com.devops.inventory_service.Model.RefreshToken;
import com.devops.inventory_service.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    // QUAN TRỌNG: Hàm này dùng để xóa token cũ
    @Modifying
    int deleteByUser(User user);
}