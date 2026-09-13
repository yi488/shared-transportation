package com.example.shared_transportation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 资金流水：记录用户支付、平台提成、车主收益、退款等资金变动。
 */
@Entity
@Table(name = "money_flow")
@Getter
@Setter
@NoArgsConstructor
public class MoneyFlow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联用户（租客 / 车主），平台流水可为空 */
    @Column(name = "user_id")
    private Long userId;

    /** 类型：USER_PAYMENT / PLATFORM_INCOME / OWNER_INCOME / REFUND */
    @Column(nullable = false, length = 32)
    private String type;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(length = 255)
    private String description;

    /** 关联对象 id（订单 / 支付单） */
    @Column(name = "related_id")
    private Long relatedId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
