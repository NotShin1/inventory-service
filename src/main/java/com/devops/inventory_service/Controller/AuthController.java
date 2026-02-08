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
import org.springframework.security.core.GrantedAuthority;
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

    // --- LOGIN ---
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        // 1. Check Pass
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // 2. Lấy Roles ra để nhét vào Token
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        // 3. Tạo JWT Access Token (Có chứa Roles)
        String jwt = jwtUtils.generateToken(userDetails.getUsername(), roles);

        // 4. Lấy User ID từ DB (Để tạo Refresh Token)
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 5. Tạo Refresh Token
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        return ResponseEntity.ok(new JwtResponse(
                jwt,
                refreshToken.getToken(),
                user.getId(),
                user.getUsername(),
                roles
        ));
    }

    // --- REFRESH TOKEN ---
    @PostMapping("/refreshtoken")
    public ResponseEntity<?> refreshtoken(@RequestBody TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(token -> {
                    User user = token.getUser();

                    // Logic: Tạo Refresh Token mới
                    RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user.getId());

                    // Logic MỚI: Lấy roles từ User Entity để tạo Access Token mới
                    // Lưu ý: User Entity phải map với bảng Role (tên hàm getRoles() tùy vào bro đặt trong Model)
                    List<String> roles = user.getRoles().stream()
                            .map(role -> "ROLE_" + role.getName()) // Đảm bảo có prefix ROLE_ nếu trong DB chưa có
                            // Nếu DB đã có chữ ROLE_ rồi thì chỉ cần .map(Role::getName)
                            .collect(Collectors.toList());

                    // Tạo Access Token mới chứa Roles
                    String newAccessToken = jwtUtils.generateToken(user.getUsername(), roles);

                    return ResponseEntity.ok(new TokenRefreshResponse(newAccessToken, newRefreshToken.getToken()));
                })
                .orElseThrow(() -> new RuntimeException("Refresh token không tồn tại!"));
    }
}