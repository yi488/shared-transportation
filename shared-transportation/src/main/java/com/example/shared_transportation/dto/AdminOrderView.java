package com.example.shared_transportation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 后台订单列表项。
 */
public record AdminOrderView(Long id, Long userId, String vehicleId, LocalDateTime startedAt,
                             LocalDateTime endedAt, BigDecimal cost, BigDecimal ownerIncome,
                             BigDecimal platformIncome, String status) {
}
