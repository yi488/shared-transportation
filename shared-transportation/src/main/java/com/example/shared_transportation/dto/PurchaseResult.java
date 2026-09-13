package com.example.shared_transportation.dto;

import java.math.BigDecimal;

/**
 * 订阅购买结果。qrCode 为支付宝当面付二维码内容。
 */
public record PurchaseResult(String outTradeNo, String qrCode, String message, BigDecimal amount) {
}
