package com.example.shared_transportation.controller;

import com.example.shared_transportation.common.ApiResponse;
import com.example.shared_transportation.common.BusinessException;
import com.example.shared_transportation.dto.AuthRequest;
import com.example.shared_transportation.dto.AuthResponse;
import com.example.shared_transportation.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台管理登录接口。
 */
@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AuthService authService;

    public AdminAuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        AuthResponse response = authService.login(request);
        if (!"ADMIN".equals(response.user().role())) {
            throw new BusinessException(40300, "无管理员权限");
        }
        return ApiResponse.ok(response);
    }
}
