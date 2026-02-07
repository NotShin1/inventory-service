package com.devops.inventory_service.DTO;
import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
}