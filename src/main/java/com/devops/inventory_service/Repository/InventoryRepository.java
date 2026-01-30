package com.devops.inventory_service.Repository;

import com.devops.inventory_service.Model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
}
