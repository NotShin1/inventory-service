package com.devops.inventory_service.Model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Mã đơn hàng (UUID hoặc mã business)
    @Column(nullable = false, unique = true)
    private String orderNumber;

    private Double totalAmount;

    private String status; // PENDING, PAID, CANCELLED

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // --- QUAN TRỌNG CHO BẢO MẬT IDOR ---
    // Mapping Many-to-One: Một User có nhiều Order.
    // Khi check IDOR: order.getUser().getUsername().equals(currentUser)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false) // Khóa ngoại trỏ về bảng users
    private User user;

    // Tự động set ngày tạo
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}