package com.devops.inventory_service.Controller;

import com.devops.inventory_service.Model.Inventory;
import com.devops.inventory_service.Service.ImageService; // Service mới xử lý ảnh
import com.devops.inventory_service.Service.InventoryService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.InetAddress;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final ImageService imageService; // Inject thêm service xử lý ảnh

    private final Bucket bucket = Bucket.builder()
            .addLimit(Bandwidth.classic(2, Refill.intervally(2, Duration.ofMinutes(1))))
            .build();

    // =========================================================
    // 🟢 KHU VỰC PUBLIC / USER (READ-ONLY)
    // =========================================================

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

    // ---------------------------------------------------------
    // 💣 FEATURE 7: UPLOAD ẢNH (MALWARE GATE)
    // Yêu cầu: Chỉ Admin + Check Hex Magic Number (trong Service)
    // ---------------------------------------------------------
    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> uploadImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        try {
            // 1. Lưu file vật lý (có check security Hex code)
            String savedFileName = imageService.saveImage(file);

            // 2. Update tên file vào DB
            inventoryService.updateProductImage(id, savedFileName);

            return ResponseEntity.ok(Map.of(
                    "message", "Upload ảnh thành công!",
                    "file", savedFileName
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Security Alert", "message", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Server Error", "message", e.getMessage()));
        }
    }

    // ---------------------------------------------------------
    // 🐌 FEATURE 8: EXPORT REPORT (RESOURCE KILLER PREVENTION)
    // Yêu cầu: Chỉ Admin + Rate Limit (Bucket4j)
    // ---------------------------------------------------------
    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> exportInventory() {
        // HARDENING: Rate Limit Check
        if (bucket.tryConsume(1)) {
            // Còn token -> Cho phép chạy (Giả lập task nặng)
            return ResponseEntity.ok(Map.of(
                    "status", "Processing",
                    "message", "Đang xuất báo cáo. Vui lòng check mail sau 5 phút!",
                    "note", "Task này tốn CPU lắm đấy nhé!"
            ));
        } else {
            // Hết token -> Chặn (HTTP 429)
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of(
                            "error", "Too Many Requests",
                            "message", "Bình tĩnh bro! Server đang thở oxy. Thử lại sau 1 phút."
                    ));
        }
    }

    @GetMapping("/version")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> getVersion() {
        return Map.of("version", "V3", "message", "🔥 HELLO SHIN! Đây là bản build V3 - Hardened RBAC Mode!");
    }

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