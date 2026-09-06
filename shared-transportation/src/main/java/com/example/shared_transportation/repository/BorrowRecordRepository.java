package com.example.shared_transportation.repository;

import com.example.shared_transportation.entity.BorrowRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Long> {

    Optional<BorrowRecord> findFirstByUserIdAndVehicleIdAndEndedAtIsNull(Long userId, String vehicleId);
}
