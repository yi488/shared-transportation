package com.example.shared_transportation.service;

import com.example.shared_transportation.common.BusinessException;
import com.example.shared_transportation.dto.BorrowResult;
import com.example.shared_transportation.dto.BorrowingView;
import com.example.shared_transportation.dto.ReturnView;
import com.example.shared_transportation.entity.BorrowRecord;
import com.example.shared_transportation.entity.MoneyFlow;
import com.example.shared_transportation.entity.PaymentRecord;
import com.example.shared_transportation.entity.Subscription;
import com.example.shared_transportation.entity.Vehicle;
import com.example.shared_transportation.repository.BorrowRecordRepository;
import com.example.shared_transportation.repository.MoneyFlowRepository;
import com.example.shared_transportation.repository.PaymentRecordRepository;
import com.example.shared_transportation.repository.SubscriptionRepository;
import com.example.shared_transportation.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BorrowService {

    private final VehicleRepository vehicleRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final MoneyFlowRepository moneyFlowRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final AlipayService alipayService;

    public BorrowService(VehicleRepository vehicleRepository,
                         BorrowRecordRepository borrowRecordRepository,
                         SubscriptionRepository subscriptionRepository,
                         MoneyFlowRepository moneyFlowRepository,
                         PaymentRecordRepository paymentRecordRepository,
                         AlipayService alipayService) {
        this.vehicleRepository = vehicleRepository;
        this.borrowRecordRepository = borrowRecordRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.moneyFlowRepository = moneyFlowRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.alipayService = alipayService;
    }

    @Transactional
    public BorrowResult borrow(Long userId, String vehicleId, String stationId) {
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
        record.setStatus("BORROWING");
        record.setStartStationId(stationId);
        borrowRecordRepository.save(record);

        vehicle.setStatus("rented_out");
        vehicleRepository.save(vehicle);

        return new BorrowResult("开始借用编号 " + vehicleId + " 的车辆", record.getId(), vehicleId);
    }

    /**
     * 还车申请：免费直接结算；需付费则生成支付二维码，支付成功后调用 completeOrder 完成结算。
     */
    @Transactional
    public ReturnView returnVehicle(Long userId, Long borrowId, String stationId) {
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
        record.setEndStationId(stationId);

        long overHalfHours = calcOverHalfHours(record.getStartedAt(), now);
        BigDecimal cost = calcCost(record.getQuotaUsed(), overHalfHours);
        record.setCost(cost);
        borrowRecordRepository.save(record);

        if (cost.compareTo(BigDecimal.ZERO) <= 0) {
            // 免费额度内，直接结算
            completeOrder(borrowId);
            return new ReturnView("还车成功", cost, false, null, null);
        }

        if (!alipayService.isConfigured()) {
            throw new BusinessException(50001, "未配置支付宝密钥，请在 application.yaml 填写 ALIPAY_APP_ID 等");
        }

        String outTradeNo = generateOrderTradeNo();
        PaymentRecord payment = new PaymentRecord();
        payment.setUserId(userId);
        payment.setType("ORDER");
        payment.setRelatedId(borrowId);
        payment.setAmount(cost);
        payment.setOutTradeNo(outTradeNo);
        payment.setStatus("PENDING");
        paymentRecordRepository.save(payment);

        String qrCode = alipayService.precreate(outTradeNo, cost, "邻车租借费用");
        if (qrCode == null || qrCode.isBlank()) {
            throw new BusinessException(50001, "生成支付二维码失败");
        }
        return new ReturnView("请支付租借费用", cost, true, qrCode, outTradeNo);
    }

    /**
     * 完成还车结算：标记已还车、分账、写资金流水（幂等）。
     */
    @Transactional
    public void completeOrder(Long borrowId) {
        BorrowRecord record = borrowRecordRepository.findById(borrowId)
                .orElseThrow(() -> new BusinessException(40400, "借车记录不存在"));
        if ("RETURNED".equals(record.getStatus())) {
            return;
        }

        long overHalfHours = calcOverHalfHours(record.getStartedAt(), record.getEndedAt());
        BigDecimal cost = calcCost(record.getQuotaUsed(), overHalfHours);
        BigDecimal baseAmount = record.getQuotaUsed() > 0
                ? BigDecimal.valueOf(1 + overHalfHours)
                : cost;
        BigDecimal ownerIncome = baseAmount.multiply(new BigDecimal("0.8"))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal platformIncome = baseAmount.subtract(ownerIncome)
                .setScale(2, RoundingMode.HALF_UP);

        record.setCost(cost);
        record.setOwnerIncome(ownerIncome);
        record.setPlatformIncome(platformIncome);
        record.setStatus("RETURNED");
        borrowRecordRepository.save(record);

        Vehicle vehicle = vehicleRepository.findById(record.getVehicleId())
                .orElseThrow(() -> new BusinessException(40400, "车辆不存在"));
        vehicle.setStatus("available");
        vehicleRepository.save(vehicle);

        MoneyFlow ownerFlow = new MoneyFlow();
        ownerFlow.setUserId(vehicle.getOwnerId());
        ownerFlow.setType("OWNER_INCOME");
        ownerFlow.setAmount(ownerIncome);
        ownerFlow.setDescription("车辆 " + record.getVehicleId() + " 租借收益");
        ownerFlow.setRelatedId(record.getId());
        moneyFlowRepository.save(ownerFlow);

        MoneyFlow platformFlow = new MoneyFlow();
        platformFlow.setType("PLATFORM_INCOME");
        platformFlow.setAmount(platformIncome);
        platformFlow.setDescription("订单 " + record.getId() + " 平台提成");
        platformFlow.setRelatedId(record.getId());
        moneyFlowRepository.save(platformFlow);
    }

    /**
     * 查询当前用户进行中的借车记录。
     */
    @Transactional(readOnly = true)
    public BorrowingView getBorrowing(Long userId) {
        return borrowRecordRepository.findByUserIdAndEndedAtIsNull(userId).stream()
                .findFirst()
                .map(r -> new BorrowingView(r.getId(), r.getVehicleId(),
                        r.getStartedAt(), r.getStartStationId()))
                .orElse(null);
    }

    private long calcOverHalfHours(LocalDateTime start, LocalDateTime end) {
        long minutes = Duration.between(start, end).toMinutes();
        if (minutes < 0) {
            minutes = 0;
        }
        double hours = minutes / 60.0;
        double over = Math.max(0, hours - 1.0);
        return (long) Math.ceil(over * 2);
    }

    private BigDecimal calcCost(int quotaUsed, long overHalfHours) {
        if (quotaUsed > 0) {
            return BigDecimal.valueOf(overHalfHours);
        }
        return BigDecimal.valueOf(2 + overHalfHours);
    }

    private String generateOrderTradeNo() {
        return "ORD" + System.currentTimeMillis()
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
