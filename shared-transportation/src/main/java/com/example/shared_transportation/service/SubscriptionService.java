package com.example.shared_transportation.service;

import com.example.shared_transportation.common.BusinessException;
import com.example.shared_transportation.dto.PurchaseResult;
import com.example.shared_transportation.dto.QuotaView;
import com.example.shared_transportation.entity.MoneyFlow;
import com.example.shared_transportation.entity.PaymentRecord;
import com.example.shared_transportation.entity.Subscription;
import com.example.shared_transportation.repository.MoneyFlowRepository;
import com.example.shared_transportation.repository.PaymentRecordRepository;
import com.example.shared_transportation.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final MoneyFlowRepository moneyFlowRepository;
    private final AlipayService alipayService;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               PaymentRecordRepository paymentRecordRepository,
                               MoneyFlowRepository moneyFlowRepository,
                               AlipayService alipayService) {
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.moneyFlowRepository = moneyFlowRepository;
        this.alipayService = alipayService;
    }

    @Transactional(readOnly = true)
    public QuotaView quota(Long userId) {
        List<Subscription> subscriptions = subscriptionRepository.findByUserId(userId);
        int freeCount = subscriptions.stream()
                .mapToInt(s -> s.getQuotaTotal() - s.getQuotaUsed())
                .sum();
        return new QuotaView(freeCount);
    }

    @Transactional
    public PurchaseResult purchase(Long userId, String plan) {
        BigDecimal amount;
        String label;
        if ("monthly".equals(plan)) {
            amount = new BigDecimal("30.00");
            label = "月卡";
        } else if ("quarterly".equals(plan)) {
            amount = new BigDecimal("90.00");
            label = "季卡";
        } else {
            throw new BusinessException(40000, "套餐类型错误");
        }

        if (!alipayService.isConfigured()) {
            throw new BusinessException(50001, "未配置支付宝密钥，请在 application.yaml 填写 ALIPAY_APP_ID 等");
        }

        String outTradeNo = generateOutTradeNo();
        PaymentRecord payment = new PaymentRecord();
        payment.setUserId(userId);
        payment.setType("SUBSCRIPTION");
        payment.setAmount(amount);
        payment.setOutTradeNo(outTradeNo);
        payment.setRemark(plan);
        payment.setStatus("PENDING");
        paymentRecordRepository.save(payment);

        String qrCode = alipayService.precreate(outTradeNo, amount, "邻车" + label + "购买");
        if (qrCode == null || qrCode.isBlank()) {
            throw new BusinessException(50001, "生成支付二维码失败");
        }
        return new PurchaseResult(outTradeNo, qrCode, "请使用支付宝扫码支付", amount);
    }

    /**
     * 支付成功后激活订阅额度并记录流水。
     */
    @Transactional
    public void activateSubscription(PaymentRecord payment) {
        String plan = payment.getRemark();
        int quotaTotal;
        String label;
        if ("monthly".equals(plan)) {
            quotaTotal = 30;
            label = "月卡";
        } else if ("quarterly".equals(plan)) {
            quotaTotal = 90;
            label = "季卡";
        } else {
            throw new BusinessException(40000, "套餐类型错误");
        }

        Subscription subscription = new Subscription();
        subscription.setUserId(payment.getUserId());
        subscription.setPlan(plan);
        subscription.setQuotaTotal(quotaTotal);
        subscription.setQuotaUsed(0);
        subscriptionRepository.save(subscription);

        MoneyFlow flow = new MoneyFlow();
        flow.setUserId(payment.getUserId());
        flow.setType("USER_PAYMENT");
        flow.setAmount(payment.getAmount());
        flow.setDescription(label + "购买");
        flow.setRelatedId(payment.getId());
        moneyFlowRepository.save(flow);
    }

    private String generateOutTradeNo() {
        return "SUB" + System.currentTimeMillis()
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
