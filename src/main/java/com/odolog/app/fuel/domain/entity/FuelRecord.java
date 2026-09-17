package com.odolog.app.fuel.domain.entity;

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
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 주유 한 건. 주행거리 + 리터 = 연비 (ServiceType 으로는 리터를 담을 자리가 없음) */
@Entity
@Table(name = "fuel_records")
public class FuelRecord extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "vehicle_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_fuel_records_vehicle")
    )
    private Vehicle vehicle;

    @Column(name = "fueled_at", nullable = false)
    private LocalDate fueledAt;

    /** 주유 시점 계기판 값. 직전 기록과의 차이 = 그동안 달린 거리 */
    @Column(nullable = false)
    private int odometer;

    /** 주유량(L). 최대 9999.99 — double 은 합산 시 오차 누적 */
    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal liters;

    /** 총 결제액(원). 단가가 아닌 총액 저장 — 단가 × 리터는 영수증과 어긋남 */
    @Column(name = "total_cost", nullable = false)
    private int totalCost;

    @Column(length = 255)
    private String memo;

    /**
     * 연비 재계산 기준점. 여럿이면 가장 최근 것 우선
     * 기록을 지우지 않는 이유 — 유류비 통계까지 함께 사라짐
     * ColumnDefault: ddl-auto 가 기존 행에 채울 값을 DB 구현에 맡기지 않기 위함
     */
    @ColumnDefault("false")
    @Column(name = "reset_point", nullable = false)
    private boolean resetPoint;

    protected FuelRecord() {
    }

    public FuelRecord(Vehicle vehicle, LocalDate fueledAt, int odometer, BigDecimal liters,
                      int totalCost, String memo) {
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

    public void changeTotalCost(int totalCost) {
        this.totalCost = totalCost;
    }

    public void changeMemo(String memo) {
        this.memo = memo;
    }

    public void changeResetPoint(boolean resetPoint) {
        this.resetPoint = resetPoint;
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

    public int getTotalCost() {
        return totalCost;
    }

    public String getMemo() {
        return memo;
    }

    public boolean isResetPoint() {
        return resetPoint;
    }
}
