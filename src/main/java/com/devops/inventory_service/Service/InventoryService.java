package com.devops.inventory_service.Service;

import com.devops.inventory_service.Model.Inventory;
import com.devops.inventory_service.Repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    //BỘ LỌC XSS: Chỉ cho phép text thuần, số, link cơ bản. Chặn sạch <script>, onclick...
    // Bro có thể kết hợp thêm Sanitizers.FORMATTING nếu muốn cho phép thẻ <b>, <i>
    private final PolicyFactory sanitizer = Sanitizers.FORMATTING.and(Sanitizers.LINKS);

    //BỘ LỌC INPUT: Regex chỉ cho phép chữ, số, khoảng trắng và dấu gạch ngang
    private final Pattern SAFE_INPUT_PATTERN = Pattern.compile("^[\\p{L}0-9\\s\\-]*$");

    // 1. Tạo mới (Secure Create)
    public Inventory createProduct(Inventory inventory) {
        // Rửa sạch dữ liệu trước khi lưu
        sanitizeInventoryData(inventory);
        return inventoryRepository.save(inventory);
    }

    // 2. Lấy tất cả (Read only)
    @Transactional(readOnly = true)
    public List<Inventory> getAllProducts() {
        return inventoryRepository.findAll();
    }

    // 3. Tìm kiếm an toàn (Secure Search)
    @Transactional(readOnly = true)
    public List<Inventory> searchProducts(String keyword) {
        // Validate ngay đầu vào
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }

        // Nếu chứa ký tự đặc biệt nguy hiểm (; ' " % v.v.) -> Chặn ngay
        if (!SAFE_INPUT_PATTERN.matcher(keyword).matches()) {
            throw new IllegalArgumentException("Từ khóa chứa ký tự không hợp lệ bro ơi! Đừng thử SQLi nhé.");
        }

        // Gọi JPA (đã an toàn với SQLi)
        return inventoryRepository.findByProductNameContainingIgnoreCaseOrSkuCodeContainingIgnoreCase(keyword.trim(), keyword.trim());
    }

    // 4. Update (Secure Update)
    public Inventory updateProduct(Long id, Inventory inventoryRequest) {
        Inventory existingProduct = inventoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bro ơi, tìm không thấy sản phẩm ID: " + id));

        // Rửa sạch dữ liệu mới update
        sanitizeInventoryData(inventoryRequest);

        existingProduct.setSkuCode(inventoryRequest.getSkuCode());
        existingProduct.setProductName(inventoryRequest.getProductName());
        existingProduct.setPrice(inventoryRequest.getPrice());
        // Nếu có thêm field description thì set ở đây luôn

        return inventoryRepository.save(existingProduct);
    }

    // 5. Delete
    public void deleteProduct(Long id) {
        if (!inventoryRepository.existsById(id)) {
            throw new RuntimeException("Có hàng đâu mà xóa bro, check lại ID: " + id);
        }
        inventoryRepository.deleteById(id);
    }

    // --- HELPER FUNCTION: Rửa dữ liệu ---
    private void sanitizeInventoryData(Inventory inventory) {
        // Lọc ProductName
        if (inventory.getProductName() != null) {
            String cleanName = sanitizer.sanitize(inventory.getProductName());
            // Decode lại HTML entities nếu cần, hoặc để nguyên để browser render text an toàn
            inventory.setProductName(cleanName);
        }
        // Lọc SkuCode (Thường SKU không nên có ký tự lạ)
        if (inventory.getSkuCode() != null) {
            String cleanSku = sanitizer.sanitize(inventory.getSkuCode());
            inventory.setSkuCode(cleanSku);
        }
    }

    public void updateProductImage(Long id, String fileName) {
        // 1. Tìm sản phẩm
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id " + id));

        // 2. Set tên file ảnh mới
        inventory.setImageFileName(fileName);

        // 3. Lưu xuống DB
        inventoryRepository.save(inventory);
    }
}