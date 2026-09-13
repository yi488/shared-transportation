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
 * 支付单：支付宝预下单 / 支付回调记录。
 * 覆盖订阅购买（月卡 30 元 / 季卡 90 元）以及借还结算收款。
 */
@Entity
@Table(name = "payment_record")
@Getter
@Setter
@NoArgsConstructor
public class PaymentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 支付类型：SUBSCRIPTION / ORDER */
    @Column(nullable = false, length = 32)
    private String type;

    /** 关联对象 id（订阅 id 或订单 id） */
    @Column(name = "related_id")
    private Long relatedId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /** 商户订单号（唯一） */
    @Column(name = "out_trade_no", nullable = false, unique = true, length = 64)
    private String outTradeNo;

    /** 支付宝交易号 */
    @Column(name = "trade_no", length = 64)
    private String tradeNo;

    /** 备注（订阅购买存 plan：monthly / quarterly） */
    @Column(length = 64)
    private String remark;

    /** 状态：PENDING / PAID / CLOSED / REFUNDED */
    @Column(nullable = false, length = 16)
    private String status = "PENDING";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = "PENDING";
        }
    }
}
