package com.devops.inventory_service.Repository;

import com.devops.inventory_service.Model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // Tìm danh sách đơn hàng của 1 user cụ thể (để user xem list của chính mình)
    List<Order> findByUserUsername(String username);
}