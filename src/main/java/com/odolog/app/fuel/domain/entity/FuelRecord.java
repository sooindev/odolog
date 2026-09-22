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

    /**
     * 주유량(L). 최대 9999.99 — double 은 합산 시 오차 누적
     * 모르면 비워 둘 수 있다. 0 으로 채우지 않는 이유 — 0L 을 넣었다는 말이 되고,
     * 연비가 0 으로 나누기가 된다. "안 적음" 과 "0" 은 다른 값이다(원칙 8)
     */
    @Column(precision = 6, scale = 2)
    private BigDecimal liters;

    /**
     * 총 결제액(원). 단가가 아닌 총액 저장 — 단가 × 리터는 영수증과 어긋남
     * 이쪽도 비워 둘 수 있어 int 가 아니라 Integer 다.
     * 0 으로 채우면 "0원에 넣었다" 가 되어 유류비 합계가 조용히 틀어진다
     */
    @Column(name = "total_cost")
    private Integer totalCost;

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
                      Integer totalCost, String memo) {
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
     * 합계에 더할 금액. 안 적은 기록은 0
     * 더할 때만큼은 "없음" 과 0 이 같은 뜻이라, 부르는 쪽마다 null 검사를 되풀이하지 않게
     * 여기 둔다. 반대로 단가·연비에서는 둘이 다른 뜻이라 그쪽은 null 을 그대로 본다
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
