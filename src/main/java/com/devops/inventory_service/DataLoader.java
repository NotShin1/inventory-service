package com.devops.inventory_service;

import com.devops.inventory_service.Model.*;
import com.devops.inventory_service.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class DataLoader {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository; // <--- Thêm ông thần này để nạp Order
    private final PasswordEncoder passwordEncoder;

    // --- LẤY CONFIG TỪ BIẾN MÔI TRƯỜNG (.env) ---
    @Value("${APP_ADMIN_USERNAME:admin}")
    private String adminUsername;

    @Value("${APP_ADMIN_PASSWORD:admin123}")
    private String adminPassword;

    @Bean
    @Transactional // Đảm bảo tính toàn vẹn dữ liệu
    CommandLineRunner initDatabase(InventoryRepository inventoryRepository) {
        return args -> {
            // ==========================================
            // 1. Kich hoat ROLE (Quan trọng nhất)
            // ==========================================
            if (roleRepository.count() == 0) {
                roleRepository.save(new Role(null, ERole.ROLE_USER));
                roleRepository.save(new Role(null, ERole.ROLE_ADMIN));
                System.out.println(">>> [SECURITY] Đã khởi tạo ROLE_USER và ROLE_ADMIN");
            }

            // ==========================================
            // 2. Tạo User Admin & User Thường (Để test IDOR)
            // ==========================================

            // 2.1 Tạo Admin
            if (!userRepository.existsByUsername(adminUsername)) {
                User admin = new User();
                admin.setUsername(adminUsername);
                admin.setPassword(passwordEncoder.encode(adminPassword));
                admin.setAccountNonLocked(true);

                Set<Role> roles = new HashSet<>();
                roles.add(roleRepository.findByName(ERole.ROLE_ADMIN).orElseThrow());
                admin.setRoles(roles);

                userRepository.save(admin);
                System.out.println(">>> [SECURITY] Đã tạo Admin User: " + adminUsername);
            }

            // 2.2 Tạo User Thường (user/user123) -> Dùng để đóng vai hacker check IDOR
            String normalUser = "user";
            if (!userRepository.existsByUsername(normalUser)) {
                User user = new User();
                user.setUsername(normalUser);
                user.setPassword(passwordEncoder.encode("user123")); // Pass mặc định cho user thường
                user.setAccountNonLocked(true);

                Set<Role> roles = new HashSet<>();
                roles.add(roleRepository.findByName(ERole.ROLE_USER).orElseThrow());
                user.setRoles(roles);

                userRepository.save(user);
                System.out.println(">>> [SECURITY] Đã tạo Normal User: " + normalUser);
            }

            // ==========================================
            // 3. Nạp dữ liệu Inventory (Kho)
            // ==========================================
            if (inventoryRepository.count() == 0) {
                Inventory sp1 = new Inventory();
                sp1.setSkuCode("iphone_15_pm");
                sp1.setProductName("iPhone 15 Pro Max");
                sp1.setPrice(BigDecimal.valueOf(1200));

                Inventory sp2 = new Inventory();
                sp2.setSkuCode("macbook_m3");
                sp2.setProductName("Macbook Pro M3");
                sp2.setPrice(BigDecimal.valueOf(2500));

                inventoryRepository.saveAll(List.of(sp1, sp2));
                System.out.println(">>> [DATA] Đã nạp dữ liệu kho mẫu!");
            }

            // ==========================================
            // 4. Nạp dữ liệu Order (Để test Feature 6 - IDOR)
            // ==========================================
            if (orderRepository.count() == 0) {
                User adminObj = userRepository.findByUsername(adminUsername).orElseThrow();
                User userObj = userRepository.findByUsername(normalUser).orElseThrow();

                // Đơn hàng của Admin (ID: 1)
                Order order1 = new Order();
                order1.setOrderNumber("ORD-ADMIN-001");
                order1.setTotalAmount(1200.0);
                order1.setUser(adminObj); // Của Admin
                order1.setStatus("PAID");

                // Đơn hàng của User thường (ID: 2)
                Order order2 = new Order();
                order2.setOrderNumber("ORD-USER-001");
                order2.setTotalAmount(50.0);
                order2.setUser(userObj); // Của User
                order2.setStatus("PENDING");

                orderRepository.saveAll(List.of(order1, order2));
                System.out.println(">>> [DATA] Đã tạo Order mẫu để test IDOR (1 cái của Admin, 1 cái của User)");
            }
        };
    }
}