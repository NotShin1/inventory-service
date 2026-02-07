package com.devops.inventory_service.Repository;

import com.devops.inventory_service.Model.ERole;
import com.devops.inventory_service.Model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(ERole name);
}