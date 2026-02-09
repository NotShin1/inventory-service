package com.devops.inventory_service.Controller;

import com.devops.inventory_service.Model.Inventory;
import com.devops.inventory_service.Service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    // --- KHU VỰC PUBLIC / USER (READ-ONLY) ---

    // GET: Lấy danh sách
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("isAuthenticated()")
    public List<Inventory> getAllProducts() {
        return inventoryService.getAllProducts();
    }

    // 🔥 API SEARCH (Đã thêm lại & Hardened)
    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> searchProducts(@RequestParam String keyword) {
        try {
            List<Inventory> result = inventoryService.searchProducts(keyword);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Input Error", "message", e.getMessage()));
        }
    }

    // --- KHU VỰC ADMIN (WRITE) ---

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createProduct(@RequestBody Inventory inventory) {
        Inventory created = inventoryService.createProduct(inventory);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Thêm hàng thành công!", "data", created));
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

    // --- KHU VỰC UTILITY (Hồi phục lại cái này để pass Test) ---

    // 👇 Cái hàm này nãy tui lỡ xóa nè
    @GetMapping("/version")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> getVersion() {
        return Map.of("version", "V3", "message", "🔥 HELLO SHIN! Đây là bản build V3 - Hardened RBAC Mode!");
    }

    // 👇 Cái hàm này cho admin check server
    @GetMapping("/server-info")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getServerInfo() {
        try {
            String containerId = InetAddress.getLocalHost().getHostName();
            String osName = System.getProperty("os.name");
            return ResponseEntity.ok(Map.of(
                    "containerId", containerId,
                    "os", osName,
                    "status", "Đang chạy ngon lành cành đào!"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Không lấy được thông tin server bro ơi!"));
        }
    }
}