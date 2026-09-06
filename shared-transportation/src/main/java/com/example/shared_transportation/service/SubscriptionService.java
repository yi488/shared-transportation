package com.example.shared_transportation.service;

import com.example.shared_transportation.common.BusinessException;
import com.example.shared_transportation.dto.MessageView;
import com.example.shared_transportation.dto.QuotaView;
import com.example.shared_transportation.entity.Subscription;
import com.example.shared_transportation.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
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
    public MessageView purchase(Long userId, String plan) {
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
        subscription.setUserId(userId);
        subscription.setPlan(plan);
        subscription.setQuotaTotal(quotaTotal);
        subscription.setQuotaUsed(0);
        subscriptionRepository.save(subscription);

        return new MessageView(label + "购买成功");
    }
}
