package com.devops.inventory_service.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal; // Import cái này để tính tiền không bị sai số

@Entity
@Table(name = "t_inventory")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @Column này để báo DB là: Cột này không được để trống (Not Null)
    // và cấm trùng mã SKU (Unique) -> Chuẩn bài thiết kế DB
    @Column(nullable = false, unique = true)
    private String skuCode;

    private String productName; // Tên sản phẩm (Ví dụ: iPhone 15 Pro Max)

    private String description; // Mô tả (Ví dụ: Màu Titan tự nhiên, 256GB)

    private Integer quantity; // Số lượng tồn kho

    private BigDecimal price; // Giá nhập kho (Dùng BigDecimal là chuẩn nhất cho tiền tệ)

    @Column(name = "image_file_name") // Tên cột trong DB
    private String imageFileName;     // Lưu tên file (ví dụ: uuid-123.jpg)
}