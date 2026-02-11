package com.devops.inventory_service.Service;

import com.devops.inventory_service.DTO.OrderDTO;
import com.devops.inventory_service.Model.Order;
import com.devops.inventory_service.Model.User;
import com.devops.inventory_service.Repository.OrderRepository;
import com.devops.inventory_service.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository; // Dùng để map khi tạo đơn (nếu cần)

    // Hàm lấy chi tiết đơn hàng (Có check IDOR)
    public OrderDTO getOrderById(Long orderId, String currentUsername, boolean isAdmin) {
        // 1. Tìm đơn hàng trong DB
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        // 2. 🔥 HARDENING: IDOR CHECK 🔥
        // Lấy username của người tạo đơn ra so sánh
        String ownerUsername = order.getUser().getUsername();

        // Logic: Nếu không phải chủ đơn hàng VÀ cũng không phải Admin -> CHẶN
        if (!ownerUsername.equals(currentUsername) && !isAdmin) {

            // Note: Sau này mình sẽ gắn log Monitor ở đây để báo động
            System.out.println("[SECURITY ALERT] IDOR Detected! User: " + currentUsername + " trying to access Order of: " + ownerUsername);

            throw new AccessDeniedException("Access Denied: You are not the owner of this order.");
        }

        // 3. Nếu qua được cửa trên -> Convert sang DTO trả về
        return mapToDTO(order);
    }

    // Helper: Convert Entity -> DTO
    private OrderDTO mapToDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setStatus(order.getStatus());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setCreatedBy(order.getUser().getUsername()); // Chỉ lấy username
        return dto;
    }
}