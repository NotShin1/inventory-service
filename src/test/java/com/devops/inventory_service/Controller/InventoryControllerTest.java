package com.devops.inventory_service.Controller;

import com.devops.inventory_service.Security.JwtUtils;
import com.devops.inventory_service.Service.AuthService; // Nhớ import service này nếu mock
import com.devops.inventory_service.Service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
// --- IMPORT MỚI CHO SPRING BOOT 3.4+ ---
import org.springframework.test.context.bean.override.mockito.MockitoBean;
// ---------------------------------------
import org.springframework.security.core.userdetails.UserDetailsService;
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

    // --- DÙNG @MockitoBean THAY CHO @MockBean ---

    @MockitoBean
    private InventoryService inventoryService;

    // Mock mấy cái Bean bảo mật để Test khởi động được
    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private AuthService authService; // Thêm cái này nếu Controller có gọi

    // --------------------------------------------

    @Test
    // Giả lập user 'shin' đã login với quyền USER
    @WithMockUser(username = "shin", roles = "USER")
    void shouldReturnCorrectVersion() throws Exception {
        mockMvc.perform(get("/api/inventory/version"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("V3")));
    }

    @Test
        // Test trường hợp chưa login -> Phải bị chặn (401)
    void shouldFailIfNotLogin() throws Exception {
        mockMvc.perform(get("/api/inventory/version"))
                .andExpect(status().isUnauthorized());
    }
}