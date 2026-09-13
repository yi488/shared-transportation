package com.example.shared_transportation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 租还站点（开发阶段：珠海市内 3 个停车场）。
 */
@Entity
@Table(name = "station")
@Getter
@Setter
@NoArgsConstructor
public class Station {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 255)
    private String address;

    /** 经度 */
    @Column(nullable = false)
    private Double longitude;

    /** 纬度 */
    @Column(nullable = false)
    private Double latitude;
}
