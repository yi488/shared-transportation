package com.example.shared_transportation.dto;

import java.math.BigDecimal;

/**
 * 还车结果。
 * needPay=true 时表示需支付后完成结算，qrCode 为支付宝二维码内容。
 */
public record ReturnView(String message, BigDecimal cost, boolean needPay,
                         String qrCode, String outTradeNo) {
}
