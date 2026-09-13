package com.example.shared_transportation.repository;

import com.example.shared_transportation.entity.BorrowRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Long> {

    Optional<BorrowRecord> findFirstByUserIdAndVehicleIdAndEndedAtIsNull(Long userId, String vehicleId);

    List<BorrowRecord> findByUserIdAndEndedAtIsNull(Long userId);

    long countByStartedAtAfter(LocalDateTime time);

    List<BorrowRecord> findAllByOrderByStartedAtDesc();
}
