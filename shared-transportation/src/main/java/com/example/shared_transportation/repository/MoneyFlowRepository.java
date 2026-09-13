package com.example.shared_transportation.repository;

import com.example.shared_transportation.entity.MoneyFlow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface MoneyFlowRepository extends JpaRepository<MoneyFlow, Long> {

    List<MoneyFlow> findAllByOrderByCreatedAtDesc();

    @Query("select coalesce(sum(m.amount), 0) from MoneyFlow m "
            + "where m.type in ('USER_PAYMENT','PLATFORM_INCOME') and m.createdAt >= :since")
    BigDecimal sumIncomeSince(@Param("since") LocalDateTime since);

    @Query("select coalesce(sum(m.amount), 0) from MoneyFlow m "
            + "where m.userId = :userId and m.type = 'OWNER_INCOME'")
    BigDecimal sumOwnerIncome(@Param("userId") Long userId);

    @Query("select coalesce(sum(m.amount), 0) from MoneyFlow m "
            + "where m.userId = :userId and m.type = 'WITHDRAW'")
    BigDecimal sumWithdraw(@Param("userId") Long userId);
}
