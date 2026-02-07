package com.devops.inventory_service.Service;

import com.devops.inventory_service.DTO.LoginRequest;
import com.devops.inventory_service.DTO.RegisterRequest;
import com.devops.inventory_service.Model.*;
import com.devops.inventory_service.Repository.*;
import com.devops.inventory_service.Security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    private static final int MAX_FAILED_ATTEMPTS = 5;

    // --- ĐĂNG KÝ ---
    public String register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new RuntimeException("Username này đã tồn tại!");
        }

        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setAccountNonLocked(true);

        Set<Role> roles = new HashSet<>();
        // Lúc này DataLoader đã chạy rồi, nên chắc chắn tìm thấy ROLE_USER
        Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Lỗi Server: Role chưa được khởi tạo."));
        roles.add(userRole);
        user.setRoles(roles);

        userRepository.save(user);
        return "Đăng ký thành công!";
    }

    // --- ĐĂNG NHẬP (HARDENING CORE) ---
    public String login(LoginRequest req) {
        // 1. HARDENING: Delay 1s (Chống Timing Attack)
        // Dù đúng hay sai cũng chờ 1s để hacker không đoán được username
        try { TimeUnit.SECONDS.sleep(1); } catch (InterruptedException e) {}

        User user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new RuntimeException("Thông tin đăng nhập không chính xác"));
        // Note: Thông báo chung chung, không nói rõ là sai User hay sai Pass

        // 2. HARDENING: Check Lock (Chống Brute-force)
        if (!user.isAccountNonLocked()) {
            throw new RuntimeException("Tài khoản đã bị khóa do nhập sai quá 5 lần!");
        }

        // 3. Logic Check Pass
        if (passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            // Đúng pass -> Reset bộ đếm sai về 0
            if (user.getFailedAttempt() > 0) {
                user.setFailedAttempt(0);
                userRepository.save(user);
            }
            return jwtUtils.generateToken(user.getUsername());
        } else {
            // Sai pass -> Tăng bộ đếm
            int newFail = user.getFailedAttempt() + 1;
            user.setFailedAttempt(newFail);

            // Nếu sai >= 5 lần -> Khóa luôn
            if (newFail >= MAX_FAILED_ATTEMPTS) {
                user.setAccountNonLocked(false);
            }
            userRepository.save(user); // Lưu trạng thái mới vào DB

            throw new RuntimeException("Thông tin đăng nhập không chính xác");
        }
    }
}