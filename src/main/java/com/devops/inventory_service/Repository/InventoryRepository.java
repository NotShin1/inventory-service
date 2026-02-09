package com.devops.inventory_service.Repository;

import com.devops.inventory_service.Model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    // Tìm kiếm theo Tên SP hoặc SKU Code (Không phân biệt hoa thường)
    List<Inventory> findByProductNameContainingIgnoreCaseOrSkuCodeContainingIgnoreCase(String productName, String skuCode);
}