package com.devops.inventory_service.Controller;

import com.devops.inventory_service.DTO.*; // Import đống DTO bro vừa tạo
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

    // --- ĐĂNG KÝ ---
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        // Dùng DTO RegisterRequest
        return ResponseEntity.ok(authService.register(request));
    }

    // --- ĐĂNG NHẬP (TRẢ VỀ JWT + REFRESH TOKEN) ---
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        // 1. Xác thực qua Spring Security (User/Pass)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        // 2. Set context (để Spring biết thằng này đã login)
        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // 3. Tạo Access Token (Ngắn hạn - 15p)
        String jwt = jwtUtils.generateToken(userDetails.getUsername());

        // 4. Tìm User ID để tạo Refresh Token
        User user = userRepository.findByUsername(userDetails.getUsername()).get();

        // 5. Tạo Refresh Token (Dài hạn - 7 ngày) lưu xuống DB
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        // 6. Lấy Role (để FE biết là Admin hay User)
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        // 7. Trả về DTO JwtResponse chuẩn chỉnh
        return ResponseEntity.ok(new JwtResponse(
                jwt,
                refreshToken.getToken(), // Token dài hạn
                user.getId(),
                user.getUsername(),
                roles
        ));
    }

    // --- CẤP LẠI TOKEN MỚI (DÙNG REFRESH TOKEN) ---
    @PostMapping("/refreshtoken")
    public ResponseEntity<?> refreshtoken(@RequestBody TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration) // Kiểm tra xem còn hạn không
                .map(RefreshToken::getUser) // Lấy user ra
                .map(user -> {
                    // Cấp Access Token mới (15 phút nữa)
                    String token = jwtUtils.generateToken(user.getUsername());
                    // Trả về DTO TokenRefreshResponse
                    return ResponseEntity.ok(new TokenRefreshResponse(token, requestRefreshToken));
                })
                .orElseThrow(() -> new RuntimeException("Refresh token không tồn tại trong DB hoặc đã hết hạn!"));
    }
}