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
 * 이 차량에서만 쓰는 권장 주기. 없으면 ServiceType 의 기본값
 *
 * 왜 필요한가: 주기가 enum 상수로 고정돼 있어 엔진오일이 언제나 5,000km(광유 기준)였다.
 * 합성유는 10,000~15,000km 라, 합성유를 쓰는 사람에게는 `지남` 이 늘 켜져 있는 경고등이 된다.
 * 늘 켜진 경고는 아무도 안 본다
 *
 * 차량 단위인 이유: 같은 사람이 디젤과 가솔린을 함께 몰 수 있고, 주기는 사람이 아니라 차의 성질이다
 *
 * km·개월을 따로 비울 수 있다. 한쪽만 바꾸고 싶은 경우가 실제로 있다 —
 * 합성유는 거리만 늘고 기간(6개월)은 그대로인 게 보통이다
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

    /** JdbcTypeCode(VARCHAR) 필수 — 네이티브 enum 컬럼이면 종류를 더할 때 운영만 깨진다 */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private ServiceType type;

    /** null 이면 그 기준은 기본값을 쓴다 */
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

    /** 둘 다 비었으면 덮어쓸 것이 없다 — 이런 행은 남겨 둘 이유가 없어 서비스가 지운다 */
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
