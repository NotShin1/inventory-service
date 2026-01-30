package com.devops.inventory_service;

import com.devops.inventory_service.Model.Inventory;
import com.devops.inventory_service.Repository.InventoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration // Đánh dấu đây là file cấu hình
public class DataLoader {

    @Bean
    CommandLineRunner initDatabase(InventoryRepository inventoryRepository) {
        return args -> {
            // Nếu trong kho chưa có gì (count = 0) thì mới thêm
            if (inventoryRepository.count() == 0) {
                Inventory sp1 = new Inventory();
                sp1.setSkuCode("iphone_15_pm");
                sp1.setProductName("iPhone 15 Pro Max");
                sp1.setPrice(BigDecimal.valueOf(1200));

                Inventory sp2 = new Inventory();
                sp2.setSkuCode("macbook_m3");
                sp2.setProductName("Macbook Pro M3");
                sp2.setPrice(BigDecimal.valueOf(2500));

                // Lưu một lèo vào DB
                inventoryRepository.saveAll(List.of(sp1, sp2));

                System.out.println(">>> DevOps Bro: Đã nạp dữ liệu mẫu thành công!");
            }
        };
    }
}
