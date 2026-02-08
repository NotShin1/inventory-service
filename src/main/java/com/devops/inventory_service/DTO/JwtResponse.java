package com.devops.inventory_service.DTO;

import lombok.Data;
import java.util.List;

@Data
public class JwtResponse {
    private String token; // Access Token
    private String type = "Bearer";
    private String refreshToken; // Refresh Token (Mới thêm)
    private Long id;
    private String username;
    private List<String> roles;

    public JwtResponse(String accessToken, String refreshToken, Long id, String username, List<String> roles) {
        this.token = accessToken;
        this.refreshToken = refreshToken;
        this.id = id;
        this.username = username;
        this.roles = roles;
    }
}