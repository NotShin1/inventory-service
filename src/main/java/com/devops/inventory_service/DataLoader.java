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
    private final PasswordEncoder passwordEncoder;

    // --- LẤY CONFIG TỪ BIẾN MÔI TRƯỜNG (.env) ---
    // Cấu trúc: @Value("${TEN_BIEN_TRONG_ENV : GIA_TRI_MAC_DINH}")
    @Value("${APP_ADMIN_USERNAME:admin}")
    private String adminUsername;

    @Value("${APP_ADMIN_PASSWORD:admin123}")
    private String adminPassword;

    @Bean
    @Transactional // Đảm bảo tính toàn vẹn dữ liệu
    CommandLineRunner initDatabase(InventoryRepository inventoryRepository) {
        return args -> {
            // 1. Kich hoat ROLE (Quan trọng nhất)
            if (roleRepository.count() == 0) {
                roleRepository.save(new Role(null, ERole.ROLE_USER));
                roleRepository.save(new Role(null, ERole.ROLE_ADMIN));
                System.out.println(">>> [SECURITY] Đã khởi tạo ROLE_USER và ROLE_ADMIN");
            }

            // 2. Tạo User Admin TỪ ENV (Chỉ tạo nếu chưa có)
            if (!userRepository.existsByUsername(adminUsername)) {
                User admin = new User();
                admin.setUsername(adminUsername);
                admin.setPassword(passwordEncoder.encode(adminPassword)); // Mã hoá pass từ env
                admin.setAccountNonLocked(true);

                Set<Role> roles = new HashSet<>();
                Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                        .orElseThrow(() -> new RuntimeException("Error: Role Admin not found."));
                roles.add(adminRole);
                admin.setRoles(roles);

                userRepository.save(admin);
                System.out.println(">>> [SECURITY] Đã tạo Admin User: " + adminUsername);
            } else {
                System.out.println(">>> [SECURITY] Admin User (" + adminUsername + ") đã tồn tại. Bỏ qua.");
            }

            // 3. Nạp dữ liệu mẫu cho Kho (Inventory)
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
        };
    }
}