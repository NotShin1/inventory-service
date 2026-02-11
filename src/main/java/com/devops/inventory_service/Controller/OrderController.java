package com.devops.inventory_service.Controller;

import com.devops.inventory_service.DTO.OrderDTO;
import com.devops.inventory_service.Service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    // API Xem chi tiết đơn hàng (GET /api/orders/{id})
    // @PreAuthorize đã config ở SecurityConfig rồi, nhưng thêm ở đây cho rõ ràng cũng được
    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long id) {
        // 1. Lấy thông tin User đang login từ Token
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();

        // Check xem có phải admin không (để bypass IDOR check nếu cần)
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(r -> r.getAuthority().equals("ROLE_ADMIN"));

        // 2. Gọi Service xử lý
        OrderDTO orderDTO = orderService.getOrderById(id, currentUsername, isAdmin);

        return ResponseEntity.ok(orderDTO);
    }
}