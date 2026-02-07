package com.devops.inventory_service.DTO;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    private String username;

    @NotBlank
    // Hardening: Regex bắt buộc password mạnh
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=]).{8,}$",
            message = "Pass yếu! Cần 8 ký tự, có hoa, thường, số và ký tự đặc biệt (@#$%^&+=)")
    private String password;
}