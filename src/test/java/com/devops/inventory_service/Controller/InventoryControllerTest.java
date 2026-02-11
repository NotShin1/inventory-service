package com.devops.inventory_service.Controller;

import com.devops.inventory_service.Security.JwtUtils;
import com.devops.inventory_service.Service.ImageService;
import com.devops.inventory_service.Service.InventoryService;
import com.devops.inventory_service.Security.UserDetailsServiceImpl; // Import class thật, không phải Interface
import com.devops.inventory_service.Repository.RoleRepository;
import com.devops.inventory_service.Repository.UserRepository;
import com.devops.inventory_service.Repository.InventoryRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
// Dùng MockitoBean cho Spring Boot 3.4
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventoryController.class)
public class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // --- 1. MOCK SERVICE CHÍNH ---
    @MockitoBean
    private InventoryService inventoryService;

    // --- 2. MOCK SECURITY (FIX LỖI CONTEXT) ---
    @MockitoBean
    private JwtUtils jwtUtils;

    // QUAN TRỌNG: Mock class cụ thể UserDetailsServiceImpl
    // Để SecurityConfig không phải đi tìm class thật rồi đụng vào DB
    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @MockitoBean
    private ImageService imageService;

    // --- 3. MOCK REPOSITORY (BỊT MIỆNG DATALOADER) ---
    // Vì DataLoader chạy lúc khởi động, nó sẽ đòi mấy cái này.
    // Mình mock hết để DataLoader chạy qua mà không làm gì cả.
    @MockitoBean
    private RoleRepository roleRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private InventoryRepository inventoryRepository;

    // ----------------------------------------------------

    @Test
    @WithMockUser(username = "shin", roles = "USER")
    void shouldReturnCorrectVersion() throws Exception {
        mockMvc.perform(get("/api/inventory/version"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("V3")));
    }

    @Test
    void shouldFailIfNotLogin() throws Exception {
        mockMvc.perform(get("/api/inventory/version"))
                .andExpect(status().isUnauthorized()); // 401
    }
}