package com.example.shared_transportation.controller;

import com.example.shared_transportation.common.ApiResponse;
import com.example.shared_transportation.common.SecurityUtil;
import com.example.shared_transportation.dto.PurchaseResult;
import com.example.shared_transportation.dto.SubscriptionRequest;
import com.example.shared_transportation.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping
    public ApiResponse<PurchaseResult> purchase(Authentication authentication,
                                                @Valid @RequestBody SubscriptionRequest request) {
        Long userId = SecurityUtil.currentUserId(authentication);
        return ApiResponse.ok(subscriptionService.purchase(userId, request.getPlan()));
    }
}
