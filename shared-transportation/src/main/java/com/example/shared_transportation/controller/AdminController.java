package com.example.shared_transportation.controller;

import com.example.shared_transportation.common.ApiResponse;
import com.example.shared_transportation.dto.AdminOrderView;
import com.example.shared_transportation.dto.AdminStats;
import com.example.shared_transportation.dto.AdminUserView;
import com.example.shared_transportation.dto.MessageView;
import com.example.shared_transportation.entity.MoneyFlow;
import com.example.shared_transportation.service.AdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台管理接口（需 ADMIN 角色）。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/stats")
    public ApiResponse<AdminStats> stats() {
        return ApiResponse.ok(adminService.stats());
    }

    @GetMapping("/users")
    public ApiResponse<List<AdminUserView>> users() {
        return ApiResponse.ok(adminService.listUsers());
    }

    @PutMapping("/users/{id}/ban")
    public ApiResponse<MessageView> banUser(@PathVariable Long id) {
        return ApiResponse.ok(adminService.setUserStatus(id, "BANNED"));
    }

    @PutMapping("/users/{id}/unban")
    public ApiResponse<MessageView> unbanUser(@PathVariable Long id) {
        return ApiResponse.ok(adminService.setUserStatus(id, "ACTIVE"));
    }

    @GetMapping("/orders")
    public ApiResponse<List<AdminOrderView>> orders() {
        return ApiResponse.ok(adminService.listOrders());
    }

    @PostMapping("/orders/{id}/refund")
    public ApiResponse<MessageView> refund(@PathVariable Long id) {
        return ApiResponse.ok(adminService.refundOrder(id));
    }

    @GetMapping("/money-flows")
    public ApiResponse<List<MoneyFlow>> moneyFlows() {
        return ApiResponse.ok(adminService.listMoneyFlows());
    }
}
