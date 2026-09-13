package com.example.shared_transportation.service;

import com.example.shared_transportation.common.BusinessException;
import com.example.shared_transportation.dto.BalanceView;
import com.example.shared_transportation.dto.MessageView;
import com.example.shared_transportation.entity.MoneyFlow;
import com.example.shared_transportation.entity.User;
import com.example.shared_transportation.repository.MoneyFlowRepository;
import com.example.shared_transportation.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 账户余额与提现。
 */
@Service
public class AccountService {

    private final MoneyFlowRepository moneyFlowRepository;
    private final UserRepository userRepository;
    private final AlipayService alipayService;

    public AccountService(MoneyFlowRepository moneyFlowRepository,
                          UserRepository userRepository,
                          AlipayService alipayService) {
        this.moneyFlowRepository = moneyFlowRepository;
        this.userRepository = userRepository;
        this.alipayService = alipayService;
    }

    @Transactional(readOnly = true)
    public BalanceView balance(Long userId) {
        BigDecimal income = moneyFlowRepository.sumOwnerIncome(userId);
        BigDecimal withdraw = moneyFlowRepository.sumWithdraw(userId);
        return new BalanceView(income.subtract(withdraw));
    }

    @Transactional
    public MessageView withdraw(Long userId, BigDecimal amount, String alipayAccount) {
        BigDecimal income = moneyFlowRepository.sumOwnerIncome(userId);
        BigDecimal withdrawn = moneyFlowRepository.sumWithdraw(userId);
        BigDecimal balance = income.subtract(withdrawn);
        if (balance.compareTo(amount) < 0) {
            throw new BusinessException(40000, "可提现余额不足");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(40400, "用户不存在"));
        String account = (alipayAccount == null || alipayAccount.isBlank())
                ? user.getPhone()
                : alipayAccount.trim();

        if (alipayService.isConfigured()) {
            String outBizNo = "W" + System.currentTimeMillis()
                    + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            boolean ok = alipayService.transfer(amount, account, outBizNo);
            if (!ok) {
                throw new BusinessException(50001, "提现转账失败，请稍后重试");
            }
        }

        MoneyFlow flow = new MoneyFlow();
        flow.setUserId(userId);
        flow.setType("WITHDRAW");
        flow.setAmount(amount);
        flow.setDescription("提现到支付宝 " + account);
        moneyFlowRepository.save(flow);

        return new MessageView("提现成功");
    }
}
