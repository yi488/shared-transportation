package com.example.shared_transportation.controller;

import com.example.shared_transportation.common.ApiResponse;
import com.example.shared_transportation.dto.StationView;
import com.example.shared_transportation.repository.StationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 站点列表接口（地图页 / 租车页使用）。
 */
@RestController
@RequestMapping("/api/stations")
public class StationController {

    private final StationRepository stationRepository;

    public StationController(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    @GetMapping
    public ApiResponse<List<StationView>> list() {
        return ApiResponse.ok(stationRepository.findAll().stream()
                .map(s -> new StationView(s.getId(), s.getName(), s.getAddress(),
                        s.getLongitude(), s.getLatitude()))
                .toList());
    }
}
