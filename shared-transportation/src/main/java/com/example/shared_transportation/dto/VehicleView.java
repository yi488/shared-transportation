package com.example.shared_transportation.dto;

/**
 * /api/vehicles 列表元素。
 */
public record VehicleView(String id, String name, String category, String imageUrl) {
}
