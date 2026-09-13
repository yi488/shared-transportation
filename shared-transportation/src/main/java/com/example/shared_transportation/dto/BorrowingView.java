package com.example.shared_transportation.dto;

import java.time.LocalDateTime;

/**
 * 当前进行中的借车记录。
 */
public record BorrowingView(Long borrowId, String vehicleId, LocalDateTime startedAt, String startStationId) {
}
