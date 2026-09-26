package com.odolog.app.maintenance.domain.entity;

import com.odolog.app.common.domain.entity.BaseTimeEntity;
import com.odolog.app.maintenance.domain.type.ServiceType;
import com.odolog.app.vehicle.domain.entity.Vehicle;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 차량별 권장 주기. 없으면 ServiceType 기본값
 * 차량 단위: 주기는 차의 성질
 * km·개월 개별 설정 가능(합성유는 거리만 늘리는 경우가 보통)
 */
@Entity
@Table(
        name = "service_intervals",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_service_intervals_vehicle_type",
                columnNames = {"vehicle_id", "type"}))
public class ServiceInterval extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "vehicle_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_service_intervals_vehicle"))
    private Vehicle vehicle;

    /** @JdbcTypeCode(VARCHAR) 필수. 네이티브 enum 방지 */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private ServiceType type;

    /** null 이면 기본값 */
    @Column(name = "interval_km")
    private Integer intervalKm;

    @Column(name = "interval_months")
    private Integer intervalMonths;

    protected ServiceInterval() {
    }

    public ServiceInterval(Vehicle vehicle, ServiceType type, Integer intervalKm, Integer intervalMonths) {
        this.vehicle = vehicle;
        this.type = type;
        this.intervalKm = intervalKm;
        this.intervalMonths = intervalMonths;
    }

    public void change(Integer intervalKm, Integer intervalMonths) {
        this.intervalKm = intervalKm;
        this.intervalMonths = intervalMonths;
    }

    /** 둘 다 비었는지. 빈 행은 서비스가 삭제 */
    public boolean isEmpty() {
        return intervalKm == null && intervalMonths == null;
    }

    public Long getId() {
        return id;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public ServiceType getType() {
        return type;
    }

    public Integer getIntervalKm() {
        return intervalKm;
    }

    public Integer getIntervalMonths() {
        return intervalMonths;
    }
}
