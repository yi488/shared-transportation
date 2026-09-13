package com.example.shared_transportation.dto;

/**
 * /api/me/vehicles 列表元素，以及车辆详情。
 */
public record MyVehicleView(String id, String name, String desc, String imageUrl, String status, String stationId) {
}
