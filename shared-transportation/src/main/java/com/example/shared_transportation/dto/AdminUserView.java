package com.example.shared_transportation.dto;

import java.time.LocalDateTime;

/**
 * 后台用户列表项。
 */
public record AdminUserView(Long id, String phone, String nickname, String role, String status,
                            LocalDateTime createdAt) {
}
