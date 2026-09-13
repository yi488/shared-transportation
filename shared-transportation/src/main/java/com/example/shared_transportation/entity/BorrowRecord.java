package com.example.shared_transportation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "borrow_record")
@Getter
@Setter
@NoArgsConstructor
public class BorrowRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "vehicle_id", nullable = false, length = 32)
    private String vehicleId;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(precision = 10, scale = 2)
    private BigDecimal cost;

    @Column(name = "quota_used", nullable = false)
    private Integer quotaUsed;

    /** 订单状态：BORROWING / RETURNED / REFUNDED */
    @Column(nullable = false, length = 16)
    private String status = "BORROWING";

    /** 车主收益（租还结算后） */
    @Column(name = "owner_income", precision = 10, scale = 2)
    private BigDecimal ownerIncome;

    /** 平台提成（租还结算后） */
    @Column(name = "platform_income", precision = 10, scale = 2)
    private BigDecimal platformIncome;

    /** 借车时所在站点 */
    @Column(name = "start_station_id", length = 32)
    private String startStationId;

    /** 还车时所在站点 */
    @Column(name = "end_station_id", length = 32)
    private String endStationId;
}
