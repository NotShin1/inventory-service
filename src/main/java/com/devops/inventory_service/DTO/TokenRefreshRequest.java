package com.devops.inventory_service.DTO;

import lombok.Data;

@Data
public class TokenRefreshRequest {
    private String refreshToken;
}