package com.example.shared_transportation.dto;

import java.math.BigDecimal;

/**
 * 后台仪表盘统计。
 */
public record AdminStats(long todayOrders, BigDecimal todayRevenue, long newUsersToday,
                         long totalUsers, long totalOrders, BigDecimal totalRevenue) {
}
