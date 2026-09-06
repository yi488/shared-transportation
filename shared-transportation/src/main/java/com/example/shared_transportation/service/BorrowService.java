package com.example.shared_transportation.service;

import com.example.shared_transportation.common.BusinessException;
import com.example.shared_transportation.dto.MessageView;
import com.example.shared_transportation.dto.ReturnView;
import com.example.shared_transportation.entity.BorrowRecord;
import com.example.shared_transportation.entity.Subscription;
import com.example.shared_transportation.entity.Vehicle;
import com.example.shared_transportation.repository.BorrowRecordRepository;
import com.example.shared_transportation.repository.SubscriptionRepository;
import com.example.shared_transportation.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BorrowService {

    private final VehicleRepository vehicleRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final SubscriptionRepository subscriptionRepository;

    public BorrowService(VehicleRepository vehicleRepository,
                         BorrowRecordRepository borrowRecordRepository,
                         SubscriptionRepository subscriptionRepository) {
        this.vehicleRepository = vehicleRepository;
        this.borrowRecordRepository = borrowRecordRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Transactional
    public MessageView borrow(Long userId, String vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new BusinessException(40400, "车辆不存在"));
        if (!"available".equals(vehicle.getStatus())) {
            throw new BusinessException(40900, "该车辆不可用");
        }

        boolean alreadyBorrowed = borrowRecordRepository
                .findFirstByUserIdAndVehicleIdAndEndedAtIsNull(userId, vehicleId)
                .isPresent();
        if (alreadyBorrowed) {
            throw new BusinessException(40900, "你已借用该车辆，请先归还");
        }

        // 优先消耗免费额度（月卡 / 季卡）
        int quotaUsed = 0;
        List<Subscription> activeSubscriptions = subscriptionRepository.findActiveByUserId(userId);
        if (!activeSubscriptions.isEmpty()) {
            Subscription subscription = activeSubscriptions.get(0);
            subscription.setQuotaUsed(subscription.getQuotaUsed() + 1);
            subscriptionRepository.save(subscription);
            quotaUsed = 1;
        }

        BorrowRecord record = new BorrowRecord();
        record.setUserId(userId);
        record.setVehicleId(vehicleId);
        record.setStartedAt(LocalDateTime.now());
        record.setQuotaUsed(quotaUsed);
        borrowRecordRepository.save(record);

        vehicle.setStatus("rented_out");
        vehicleRepository.save(vehicle);

        return new MessageView("开始借用编号 " + vehicleId + " 的车辆");
    }

    @Transactional
    public ReturnView returnVehicle(Long userId, Long borrowId) {
        BorrowRecord record = borrowRecordRepository.findById(borrowId)
                .orElseThrow(() -> new BusinessException(40400, "借车记录不存在"));
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException(40300, "无权操作该借车记录");
        }
        if (record.getEndedAt() != null) {
            throw new BusinessException(40900, "该车辆已归还");
        }

        LocalDateTime now = LocalDateTime.now();
        record.setEndedAt(now);
        BigDecimal cost = calcCost(record.getStartedAt(), now, record.getQuotaUsed());
        record.setCost(cost);
        borrowRecordRepository.save(record);

        vehicleRepository.findById(record.getVehicleId()).ifPresent(vehicle -> {
            vehicle.setStatus("available");
            vehicleRepository.save(vehicle);
        });

        return new ReturnView("还车成功", cost);
    }

    /**
     * 计价规则：
     * - 按小时（无免费额度）：起始 2 元，每多半小时 +1 元。
     * - 使用免费额度：1 小时内免费，超出部分每半小时 +1 元。
     */
    private BigDecimal calcCost(LocalDateTime start, LocalDateTime end, int quotaUsed) {
        long minutes = Duration.between(start, end).toMinutes();
        if (minutes < 0) {
            minutes = 0;
        }
        double hours = minutes / 60.0;

        if (quotaUsed > 0) {
            double over = Math.max(0, hours - 1.0);
            long halfHours = (long) Math.ceil(over * 2);
            return BigDecimal.valueOf(halfHours);
        } else {
            double over = Math.max(0, hours - 1.0);
            long halfHours = (long) Math.ceil(over * 2);
            return BigDecimal.valueOf(2 + halfHours);
        }
    }
}
