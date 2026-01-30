package com.devops.inventory_service.Service;


import com.devops.inventory_service.Model.Inventory;
import com.devops.inventory_service.Repository.InventoryRepository;// Import repository
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor // Tự động tạo constructor injection
@Transactional // Quản lý transaction cho database an toàn
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    // 1. Tạo mới
    public void createProduct(Inventory inventory) {
        inventoryRepository.save(inventory);
    }

    // 2. Lấy tất cả (Read only cho nhanh)
    @Transactional(readOnly = true)
    public List<Inventory> getAllProducts() {
        return inventoryRepository.findAll();
    }

    // 3. Update
    public Inventory updateProduct(Long id, Inventory inventoryRequest) {
        // Tìm hàng, không thấy thì chửi (throw exception) :D
        Inventory existingProduct = inventoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bro ơi, tìm không thấy sản phẩm ID: " + id));

        // Update từng trường
        existingProduct.setSkuCode(inventoryRequest.getSkuCode());
        existingProduct.setProductName(inventoryRequest.getProductName());
        existingProduct.setPrice(inventoryRequest.getPrice());

        // Lưu lại
        return inventoryRepository.save(existingProduct);
    }

    // 4. Delete
    public void deleteProduct(Long id) {
        if (!inventoryRepository.existsById(id)) {
            throw new RuntimeException("Có hàng đâu mà xóa bro, check lại ID: " + id);
        }
        inventoryRepository.deleteById(id);
    }
}