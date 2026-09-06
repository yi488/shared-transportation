package com.example.shared_transportation.controller;

import com.example.shared_transportation.common.ApiResponse;
import com.example.shared_transportation.common.SecurityUtil;
import com.example.shared_transportation.dto.BorrowRequest;
import com.example.shared_transportation.dto.MessageView;
import com.example.shared_transportation.dto.MyVehicleView;
import com.example.shared_transportation.dto.ReturnRequest;
import com.example.shared_transportation.dto.ReturnView;
import com.example.shared_transportation.dto.VehicleView;
import com.example.shared_transportation.service.BorrowService;
import com.example.shared_transportation.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class VehicleController {

    private final VehicleService vehicleService;
    private final BorrowService borrowService;

    public VehicleController(VehicleService vehicleService, BorrowService borrowService) {
        this.vehicleService = vehicleService;
        this.borrowService = borrowService;
    }

    @GetMapping("/vehicles")
    public ApiResponse<List<VehicleView>> list(@RequestParam(required = false) String category) {
        return ApiResponse.ok(vehicleService.listAvailable(category));
    }

    @GetMapping("/vehicles/{id}")
    public ApiResponse<MyVehicleView> detail(@PathVariable String id) {
        return ApiResponse.ok(vehicleService.detail(id));
    }

    @PostMapping("/borrow")
    public ApiResponse<MessageView> borrow(Authentication authentication,
                                           @Valid @RequestBody BorrowRequest request) {
        Long userId = SecurityUtil.currentUserId(authentication);
        return ApiResponse.ok(borrowService.borrow(userId, request.getVehicleId()));
    }

    @PostMapping("/return")
    public ApiResponse<ReturnView> returnVehicle(Authentication authentication,
                                                 @Valid @RequestBody ReturnRequest request) {
        Long userId = SecurityUtil.currentUserId(authentication);
        return ApiResponse.ok(borrowService.returnVehicle(userId, request.getBorrowId()));
    }
}
