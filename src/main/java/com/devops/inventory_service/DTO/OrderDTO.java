package com.devops.inventory_service.DTO;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OrderDTO {
    private Long id;
    private String orderNumber;
    private Double totalAmount;
    private String status;
    private LocalDateTime createdAt;

    // Chỉ trả về username, không trả về password hay ID
    private String createdBy;
}