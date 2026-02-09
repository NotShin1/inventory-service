package com.devops.inventory_service.Controller;

import com.devops.inventory_service.Model.Inventory;
import com.devops.inventory_service.Service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    // --- PUBLIC / USER AREA ---

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("isAuthenticated()") // Yêu cầu login
    public List<Inventory> getAllProducts() {
        return inventoryService.getAllProducts();
    }

    // 🔥 API SEARCH MỚI
    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> searchProducts(@RequestParam String keyword) {
        try {
            List<Inventory> result = inventoryService.searchProducts(keyword);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            // Trả về 400 Bad Request nếu Input Validation fail
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Input Error", "message", e.getMessage()));
        }
    }

    // --- ADMIN AREA ---

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createProduct(@RequestBody Inventory inventory) {
        Inventory created = inventoryService.createProduct(inventory);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Thêm hàng thành công (Clean Data)!", "data", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @RequestBody Inventory inventory) {
        Inventory updated = inventoryService.updateProduct(id, inventory);
        return ResponseEntity.ok(Map.of("message", "Update ngon lành!", "data", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        inventoryService.deleteProduct(id);
        return ResponseEntity.ok(Map.of("message", "Đã xóa bay màu sản phẩm ID: " + id));
    }
}