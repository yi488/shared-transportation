package com.example.shared_transportation.service;

import com.example.shared_transportation.common.BusinessException;
import com.example.shared_transportation.entity.PaymentRecord;
import com.example.shared_transportation.repository.PaymentRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 支付成功统一处理：标记支付单已支付，并按类型分发业务。
 */
@Service
public class PaymentService {

    private final PaymentRecordRepository paymentRecordRepository;
    private final SubscriptionService subscriptionService;
    private final BorrowService borrowService;

    public PaymentService(PaymentRecordRepository paymentRecordRepository,
                          SubscriptionService subscriptionService,
                          BorrowService borrowService) {
        this.paymentRecordRepository = paymentRecordRepository;
        this.subscriptionService = subscriptionService;
        this.borrowService = borrowService;
    }


    /**
     * 标记支付成功（幂等），并执行对应业务。
     */
    @Transactional
    public void markPaid(String outTradeNo, String tradeNo) {
        PaymentRecord payment = paymentRecordRepository.findByOutTradeNo(outTradeNo)
                .orElseThrow(() -> new BusinessException(40400, "支付单不存在"));
        if ("PAID".equals(payment.getStatus())) {
            return;
        }

        payment.setStatus("PAID");
        if (tradeNo != null && !tradeNo.isBlank()) {
            payment.setTradeNo(tradeNo);
        }
        payment.setPaidAt(LocalDateTime.now());
        paymentRecordRepository.save(payment);

        if ("SUBSCRIPTION".equals(payment.getType())) {
            subscriptionService.activateSubscription(payment);
        } else if ("ORDER".equals(payment.getType())) {
            borrowService.completeOrder(payment.getRelatedId());
        }
    }
}
