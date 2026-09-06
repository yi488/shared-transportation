package com.example.shared_transportation.controller;

import com.example.shared_transportation.common.ApiResponse;
import com.example.shared_transportation.common.BusinessException;
import com.example.shared_transportation.common.SecurityUtil;
import com.example.shared_transportation.dto.MyVehicleView;
import com.example.shared_transportation.dto.QuotaView;
import com.example.shared_transportation.dto.VehicleUpdateRequest;
import com.example.shared_transportation.service.SubscriptionService;
import com.example.shared_transportation.service.VehicleService;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final VehicleService vehicleService;
    private final SubscriptionService subscriptionService;

    public MeController(VehicleService vehicleService,
                        SubscriptionService subscriptionService) {
        this.vehicleService = vehicleService;
        this.subscriptionService = subscriptionService;
    }

    @GetMapping("/vehicles")
    public ApiResponse<List<MyVehicleView>> myVehicles(Authentication authentication) {
        return ApiResponse.ok(vehicleService.listMine(SecurityUtil.currentUserId(authentication)));
    }

    @PostMapping(value = "/vehicles", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<MyVehicleView> createVehicle(
            Authentication authentication,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "desc", required = false) String desc,
            @RequestParam(value = "category", required = false) String category) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(40000, "请上传车辆图片");
        }
        if (desc == null || desc.isBlank()) {
            throw new BusinessException(40000, "请填写车辆介绍");
        }
        Long userId = SecurityUtil.currentUserId(authentication);
        return ApiResponse.ok(vehicleService.create(userId, file, name, desc, category));
    }

    @PutMapping("/vehicles/{id}")
    public ApiResponse<MyVehicleView> updateVehicle(
            Authentication authentication,
            @PathVariable String id,
            @RequestBody VehicleUpdateRequest request) {
        Long userId = SecurityUtil.currentUserId(authentication);
        return ApiResponse.ok(vehicleService.update(userId, id, request));
    }

    @DeleteMapping("/vehicles/{id}")
    public ApiResponse<Void> deleteVehicle(Authentication authentication,
                                           @PathVariable String id) {
        Long userId = SecurityUtil.currentUserId(authentication);
        vehicleService.delete(userId, id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/quota")
    public ApiResponse<QuotaView> quota(Authentication authentication) {
        return ApiResponse.ok(subscriptionService.quota(SecurityUtil.currentUserId(authentication)));
    }
}
