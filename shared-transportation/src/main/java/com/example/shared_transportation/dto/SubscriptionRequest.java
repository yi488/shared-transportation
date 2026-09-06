package com.example.shared_transportation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SubscriptionRequest {

    @NotBlank(message = "套餐类型不能为空")
    @Pattern(regexp = "monthly|quarterly", message = "套餐类型错误")
    private String plan;
}
