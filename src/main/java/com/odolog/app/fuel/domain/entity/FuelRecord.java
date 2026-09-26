package com.odolog.app.fuel.domain.entity;

import com.odolog.app.common.domain.identifier.PublicId;
import com.odolog.app.common.domain.entity.BaseTimeEntity;
import com.odolog.app.vehicle.domain.entity.Vehicle;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 주유 한 건 */
@Entity
@Table(
        name = "fuel_records",
        uniqueConstraints = @UniqueConstraint(name = "uk_fuel_records_public_id", columnNames = "public_id"))
public class FuelRecord extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** URL·API 용 공개 id(규칙 9-1) */
    @Column(name = "public_id", nullable = false, updatable = false, length = PublicId.LENGTH)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "vehicle_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_fuel_records_vehicle")
    )
    private Vehicle vehicle;

    @Column(name = "fueled_at", nullable = false)
    private LocalDate fueledAt;

    /** 주유 시점 계기판 값 */
    @Column(nullable = false)
    private int odometer;

    /**
     * 주유량(L), 최대 9999.99. double 은 합산 오차
     * 비울 수 있음. 0 은 "0L 주유" 라는 다른 뜻(원칙 8)
     */
    @Column(precision = 6, scale = 2)
    private BigDecimal liters;

    /**
     * 총 결제액(원). 단가 대신 총액 저장, 영수증과 일치
     * 비울 수 있어 Integer
     */
    @Column(name = "total_cost")
    private Integer totalCost;

    @Column(length = 255)
    private String memo;

    /**
     * 연비 재계산 기준점. 여럿이면 최근 것 우선
     * @ColumnDefault: 기존 행의 기본값 명시
     */
    @ColumnDefault("false")
    @Column(name = "reset_point", nullable = false)
    private boolean resetPoint;

    protected FuelRecord() {
    }

    public FuelRecord(Vehicle vehicle, LocalDate fueledAt, int odometer, BigDecimal liters,
                      Integer totalCost, String memo) {
        this.publicId = PublicId.generate();
        this.vehicle = vehicle;
        this.fueledAt = fueledAt;
        this.odometer = odometer;
        this.liters = liters;
        this.totalCost = totalCost;
        this.memo = memo;
    }

    public void changeFueledAt(LocalDate fueledAt) {
        this.fueledAt = fueledAt;
    }

    public void changeOdometer(int odometer) {
        this.odometer = odometer;
    }

    public void changeLiters(BigDecimal liters) {
        this.liters = liters;
    }

    public void changeTotalCost(Integer totalCost) {
        this.totalCost = totalCost;
    }

    public void changeMemo(String memo) {
        this.memo = memo;
    }

    public void changeResetPoint(boolean resetPoint) {
        this.resetPoint = resetPoint;
    }

    public String getPublicId() {
        return publicId;
    }

    public Long getId() {
        return id;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public LocalDate getFueledAt() {
        return fueledAt;
    }

    public int getOdometer() {
        return odometer;
    }

    public BigDecimal getLiters() {
        return liters;
    }

    public Integer getTotalCost() {
        return totalCost;
    }

    /**
     * 합계용 금액. 안 적은 기록은 0
     * 단가·연비에서는 null 그대로 사용
     */
    public int totalCostOrZero() {
        return totalCost == null ? 0 : totalCost;
    }

    public String getMemo() {
        return memo;
    }

    public boolean isResetPoint() {
        return resetPoint;
    }
}
