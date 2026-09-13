package com.example.shared_transportation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WithdrawRequest {

    @NotNull(message = "提现金额不能为空")
    @DecimalMin(value = "0.01", message = "提现金额无效")
    private BigDecimal amount;

    /** 支付宝账号（默认使用注册手机号） */
    private String alipayAccount;
}
