package com.example.shared_transportation.dto;

/**
 * 站点信息。
 */
public record StationView(String id, String name, String address, Double longitude, Double latitude) {
}
