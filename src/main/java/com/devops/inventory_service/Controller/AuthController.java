package com.devops.inventory_service.Controller;

import com.devops.inventory_service.DTO.*;
import com.devops.inventory_service.Model.RefreshToken;
import com.devops.inventory_service.Model.User;
import com.devops.inventory_service.Repository.UserRepository;
import com.devops.inventory_service.Security.JwtUtils;
import com.devops.inventory_service.Service.AuthService;
import com.devops.inventory_service.Service.RefreshTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AuthService authService;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    // --- LOGIN: Fix lỗi Duplicate Key ---
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        // 1. Check Pass
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // 2. Tạo JWT Access Token
        String jwt = jwtUtils.generateToken(userDetails.getUsername());

        // 3. Lấy User ID từ DB (Để tạo Refresh Token)
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 4. Tạo Refresh Token (Hàm này tự xóa cũ -> tạo mới)
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        // 5. Lấy Roles
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        return ResponseEntity.ok(new JwtResponse(
                jwt,
                refreshToken.getToken(),
                user.getId(),
                user.getUsername(),
                roles
        ));
    }

    // --- REFRESH TOKEN: Rotation (Đổi cũ lấy mới) ---
    @PostMapping("/refreshtoken")
    public ResponseEntity<?> refreshtoken(@RequestBody TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(token -> {
                    // Lấy User từ token cũ
                    User user = token.getUser();

                    // Gọi hàm này: Nó sẽ TỰ ĐỘNG XÓA TOKEN CŨ (của User này) và TẠO MỚI
                    // Không cần gọi deleteByToken thủ công nữa vì createRefreshToken đã có deleteByUser rồi
                    RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user.getId());

                    String newAccessToken = jwtUtils.generateToken(user.getUsername());

                    return ResponseEntity.ok(new TokenRefreshResponse(newAccessToken, newRefreshToken.getToken()));
                })
                .orElseThrow(() -> new RuntimeException("Refresh token không tồn tại hoặc đã bị thu hồi!"));
    }
}