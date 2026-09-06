package com.example.shared_transportation.repository;

import com.example.shared_transportation.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, String> {

    List<Vehicle> findByStatusOrderByCreatedAtDesc(String status);

    List<Vehicle> findByStatusAndCategoryOrderByCreatedAtDesc(String status, String category);

    List<Vehicle> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}
