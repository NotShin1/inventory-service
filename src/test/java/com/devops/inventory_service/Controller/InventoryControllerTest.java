package com.devops.inventory_service.Controller;

import com.devops.inventory_service.Service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.hamcrest.Matchers;

@WebMvcTest(InventoryController.class)
public class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryService inventoryService;

    @Test
    public void shouldReturnCorrectVersion() throws Exception {
        // 1. Giả lập gọi vào đường dẫn API
        mockMvc.perform(MockMvcRequestBuilders.get("/api/inventory/version"))

                // 2. Mong đợi Status Code là 200 (OK)
                .andExpect(MockMvcResultMatchers.status().isOk())

                // 3. Mong đợi nội dung trả về có chứa chữ "V2"
                .andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("V2")));
    }
}