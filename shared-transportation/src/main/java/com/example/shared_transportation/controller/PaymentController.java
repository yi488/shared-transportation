package com.example.shared_transportation.controller;

import com.example.shared_transportation.common.ApiResponse;
import com.example.shared_transportation.common.BusinessException;
import com.example.shared_transportation.dto.PayStatusView;
import com.example.shared_transportation.entity.PaymentRecord;
import com.example.shared_transportation.repository.PaymentRecordRepository;
import com.example.shared_transportation.service.AlipayService;
import com.example.shared_transportation.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝支付回调与状态查询。
 */
@RestController
@RequestMapping("/api/pay")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final AlipayService alipayService;
    private final PaymentService paymentService;
    private final PaymentRecordRepository paymentRecordRepository;

    public PaymentController(AlipayService alipayService,
                             PaymentService paymentService,
                             PaymentRecordRepository paymentRecordRepository) {
        this.alipayService = alipayService;
        this.paymentService = paymentService;
        this.paymentRecordRepository = paymentRecordRepository;
    }

    /**
     * 支付宝异步通知（无需登录）。
     * 无论是真实支付还是模拟支付，都走此接口。
     */
    @PostMapping("/notify")
    public String notify(HttpServletRequest request) {
        Map<String, String> params = parseParams(request);
        log.info("收到支付异步通知，参数：{}", params);

        // 验签（模拟模式下自动返回 true）
        if (!alipayService.verifyNotify(params)) {
            log.warn("异步通知验签失败，参数：{}", params);
            return "fail";
        }

        String outTradeNo = params.get("out_trade_no");
        String tradeStatus = params.get("trade_status");
        String tradeNo = params.get("trade_no");

        // 注意：模拟支付模式下，trade_no 可能为空，此时由业务层自动生成模拟流水号
        if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
            try {
                paymentService.markPaid(outTradeNo, tradeNo);
                log.info("订单 {} 支付成功，支付宝交易号：{}", outTradeNo, tradeNo);
            } catch (Exception e) {
                log.error("处理支付成功回调失败，订单号：{}", outTradeNo, e);
                return "fail";
            }
        } else {
            log.info("订单 {} 当前状态：{}，暂不处理", outTradeNo, tradeStatus);
        }

        // 必须返回 success，否则支付宝会持续重发通知
        return "success";
    }

    /**
     * 查询支付状态（前端轮询）。
     * 真实模式：主动向支付宝查询最新状态并更新本地记录。
     * 模拟模式：直接标记为支付成功（模拟支付不需要真实回调）。
     */
    @GetMapping("/status")
    public ApiResponse<PayStatusView> status(@RequestParam String outTradeNo) {
        log.info("查询支付状态，订单号：{}", outTradeNo);

        // 1. 查询本地支付记录
        PaymentRecord payment = paymentRecordRepository.findByOutTradeNo(outTradeNo)
                .orElseThrow(() -> new BusinessException(40400, "支付单不存在"));

        // 2. 如果已经支付成功，直接返回
        if ("PAID".equals(payment.getStatus()) || "SUCCESS".equals(payment.getStatus())) {
            return ApiResponse.ok(new PayStatusView(payment.getStatus(), true));
        }

        // 3. 如果状态为 PENDING（待支付），尝试查询最新状态
        if ("PENDING".equals(payment.getStatus()) || "WAIT_PAY".equals(payment.getStatus())) {
            // 3.1 真实支付模式：向支付宝查询
            if (alipayService.isRealMode()) {
                try {
                    String tradeStatus = alipayService.queryTrade(outTradeNo);
                    log.info("支付宝查询结果，订单号：{}，交易状态：{}", outTradeNo, tradeStatus);
                    if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                        paymentService.markPaid(outTradeNo, null);
                        log.info("通过主动查询确认订单 {} 已支付", outTradeNo);
                    }
                } catch (Exception e) {
                    log.error("主动查询支付宝订单状态失败，订单号：{}", outTradeNo, e);
                    // 查询失败不抛出异常，返回当前本地状态（前端继续轮询）
                }
            } else {
                // 3.2 模拟支付模式：直接标记为支付成功
                log.info("模拟支付模式，自动将订单 {} 标记为已支付", outTradeNo);
                paymentService.markPaid(outTradeNo, "MOCK_TRADE_" + System.currentTimeMillis());
            }
        }

        // 4. 重新查询并返回最新状态
        PaymentRecord refreshed = paymentRecordRepository.findByOutTradeNo(outTradeNo)
                .orElse(payment);
        boolean paid = "PAID".equals(refreshed.getStatus()) || "SUCCESS".equals(refreshed.getStatus());
        return ApiResponse.ok(new PayStatusView(refreshed.getStatus(), paid));
    }

    /**
     * 解析请求参数（支持 GET/POST 表
     * 单提交）。
     */
    private Map<String, String> parseParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            String[] values = entry.getValue();
            if (values != null && values.length > 0) {
                params.put(entry.getKey(), values[0]);
            }
        }
        return params;
    }
}