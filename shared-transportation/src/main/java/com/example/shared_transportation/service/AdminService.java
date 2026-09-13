package com.example.shared_transportation.service;

import com.example.shared_transportation.common.BusinessException;
import com.example.shared_transportation.dto.AdminOrderView;
import com.example.shared_transportation.dto.AdminStats;
import com.example.shared_transportation.dto.AdminUserView;
import com.example.shared_transportation.dto.MessageView;
import com.example.shared_transportation.entity.BorrowRecord;
import com.example.shared_transportation.entity.MoneyFlow;
import com.example.shared_transportation.entity.User;
import com.example.shared_transportation.repository.BorrowRecordRepository;
import com.example.shared_transportation.repository.MoneyFlowRepository;
import com.example.shared_transportation.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台管理业务。
 */
@Service
public class AdminService {

    private final UserRepository userRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final MoneyFlowRepository moneyFlowRepository;

    public AdminService(UserRepository userRepository,
                        BorrowRecordRepository borrowRecordRepository,
                        MoneyFlowRepository moneyFlowRepository) {
        this.userRepository = userRepository;
        this.borrowRecordRepository = borrowRecordRepository;
        this.moneyFlowRepository = moneyFlowRepository;
    }

    @Transactional(readOnly = true)
    public AdminStats stats() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime epoch = LocalDateTime.of(1970, 1, 1, 0, 0);

        long todayOrders = borrowRecordRepository.countByStartedAtAfter(todayStart);
        BigDecimal todayRevenue = moneyFlowRepository.sumIncomeSince(todayStart);
        long newUsersToday = userRepository.countByCreatedAtAfter(todayStart);
        long totalUsers = userRepository.count();
        long totalOrders = borrowRecordRepository.count();
        BigDecimal totalRevenue = moneyFlowRepository.sumIncomeSince(epoch);

        return new AdminStats(todayOrders, todayRevenue, newUsersToday,
                totalUsers, totalOrders, totalRevenue);
    }

    @Transactional(readOnly = true)
    public List<AdminUserView> listUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toUserView)
                .toList();
    }

    @Transactional
    public MessageView setUserStatus(Long userId, String status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(40400, "用户不存在"));
        user.setStatus(status);
        userRepository.save(user);
        return new MessageView("BANNED".equals(status) ? "已封禁" : "已解封");
    }

    @Transactional(readOnly = true)
    public List<AdminOrderView> listOrders() {
        return borrowRecordRepository.findAllByOrderByStartedAtDesc().stream()
                .map(this::toOrderView)
                .toList();
    }

    @Transactional
    public MessageView refundOrder(Long orderId) {
        BorrowRecord record = borrowRecordRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(40400, "订单不存在"));
        if ("REFUNDED".equals(record.getStatus())) {
            throw new BusinessException(40900, "订单已退款");
        }
        if (record.getEndedAt() == null) {
            throw new BusinessException(40900, "订单进行中，无法退款");
        }
        record.setStatus("REFUNDED");
        borrowRecordRepository.save(record);

        MoneyFlow flow = new MoneyFlow();
        flow.setUserId(record.getUserId());
        flow.setType("REFUND");
        flow.setAmount(record.getCost() == null ? BigDecimal.ZERO : record.getCost());
        flow.setDescription("订单 " + orderId + " 退款");
        flow.setRelatedId(orderId);
        moneyFlowRepository.save(flow);

        return new MessageView("退款成功");
    }

    @Transactional(readOnly = true)
    public List<MoneyFlow> listMoneyFlows() {
        return moneyFlowRepository.findAllByOrderByCreatedAtDesc();
    }

    private AdminUserView toUserView(User u) {
        return new AdminUserView(u.getId(), u.getPhone(), u.getNickname(),
                u.getRole(), u.getStatus(), u.getCreatedAt());
    }

    private AdminOrderView toOrderView(BorrowRecord r) {
        return new AdminOrderView(r.getId(), r.getUserId(), r.getVehicleId(),
                r.getStartedAt(), r.getEndedAt(), r.getCost(),
                r.getOwnerIncome(), r.getPlatformIncome(), r.getStatus());
    }
}
