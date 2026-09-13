package com.example.shared_transportation.repository;

import com.example.shared_transportation.entity.Station;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StationRepository extends JpaRepository<Station, String> {
}
