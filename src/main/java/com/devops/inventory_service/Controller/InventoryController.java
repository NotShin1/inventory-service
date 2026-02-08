package com.devops.inventory_service.Controller;

import com.devops.inventory_service.Model.Inventory;
import com.devops.inventory_service.Service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity; // Dùng cái này return chuẩn hơn
import org.springframework.security.access.prepost.PreAuthorize; // Quan trọng
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

    // GET: Xem hàng - Ai đã login (User/Admin/Staff) đều xem được
    // Hardening: isAuthenticated() đảm bảo phải có Token hợp lệ mới được vào
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("isAuthenticated()")
    public List<Inventory> getAllProducts() {
        return inventoryService.getAllProducts();
    }

    // --- KHU VỰC ADMIN (WRITE/EDIT/DELETE) ---

    // POST: Thêm hàng - Chỉ Admin
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createProduct(@RequestBody Inventory inventory) {
        inventoryService.createProduct(inventory);
        // Trả về JSON đẹp hơn là String trần
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Đã thêm hàng thành công nha bro!", "status", "success"));
    }

    // PUT: Sửa hàng - Chỉ Admin
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @RequestBody Inventory inventory) {
        Inventory updated = inventoryService.updateProduct(id, inventory);
        return ResponseEntity.ok(updated);
    }

    // DELETE: Xóa hàng - Chỉ Admin (Cực kỳ nhạy cảm)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        inventoryService.deleteProduct(id);
        return ResponseEntity.ok(Map.of("message", "Đã xóa sản phẩm ID: " + id + " khỏi kho!"));
    }

    // --- KHU VỰC UTILITY (SYSTEM INFO) ---

    // Version: Cho public hoặc user xem để biết deploy bản mới chưa
    @GetMapping("/version")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> getVersion() {
        return Map.of("version", "V3", "message", "🔥 HELLO SHIN! Đây là bản build V3 - Hardened RBAC Mode!");
    }

    // Server Info: Thường cái này nhạy cảm, chỉ nên cho Admin xem IP/OS thôi
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