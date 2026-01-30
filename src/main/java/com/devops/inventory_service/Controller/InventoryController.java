package com.devops.inventory_service.Controller;


import com.devops.inventory_service.Model.Inventory;
import com.devops.inventory_service.Service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    // POST: Thêm hàng
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // Trả về 201 Created cho chuẩn
    public String createProduct(@RequestBody Inventory inventory) {
        inventoryService.createProduct(inventory);
        return "Đã thêm hàng thành công nha bro!";
    }

    // GET: Xem hàng
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<Inventory> getAllProducts() {
        return inventoryService.getAllProducts();
    }

    // PUT: Sửa hàng
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Inventory updateProduct(@PathVariable Long id, @RequestBody Inventory inventory) {
        return inventoryService.updateProduct(id, inventory);
    }

    // DELETE: Xóa hàng
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public String deleteProduct(@PathVariable Long id) {
        inventoryService.deleteProduct(id);
        return "Đã xóa sản phẩm ID: " + id + " khỏi kho!";
    }
}
