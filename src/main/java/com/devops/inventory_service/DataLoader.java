package com.devops.inventory_service;

import com.devops.inventory_service.Model.*;
import com.devops.inventory_service.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class DataLoader {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // Cần cái này để mã hóa pass admin

    @Bean
    CommandLineRunner initDatabase(InventoryRepository inventoryRepository) {
        return args -> {
            // 1. NẠP ROLE (Quan trọng nhất - không có cái này Login/Signup sập nguồn)
            if (roleRepository.count() == 0) {
                roleRepository.save(new Role(null, ERole.ROLE_USER));
                roleRepository.save(new Role(null, ERole.ROLE_ADMIN));
                System.out.println(">>> SECURITY: Đã khởi tạo ROLE_USER và ROLE_ADMIN");
            }

            // 2. NẠP USER ADMIN MẶC ĐỊNH (Để bro test ngay không cần đăng ký)
            if (!userRepository.existsByUsername("admin")) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("Admin@123")); // Pass cứng, nhớ đổi sau
                admin.setAccountNonLocked(true);

                Set<Role> roles = new HashSet<>();
                Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN).get();
                roles.add(adminRole);
                admin.setRoles(roles);

                userRepository.save(admin);
                System.out.println(">>> SECURITY: Đã tạo tài khoản Admin (User: admin / Pass: Admin@123)");
            }

            // 3. NẠP INVENTORY (Dữ liệu cũ của bro)
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
                System.out.println(">>> DATA: Đã nạp dữ liệu kho mẫu!");
            }
        };
    }
}