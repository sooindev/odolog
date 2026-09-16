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

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 주유 한 번의 기록. 연비 계산의 재료다.
 *
 * <p><b>왜 정비 이력과 따로 두는가.</b> 정비는 "돈을 썼다"는 기록이지만 주유는 거기에
 * <b>부피(리터)</b>가 붙는다. 주행거리와 리터가 만나야 연비가 나오고, 그게 이 엔티티의 존재 이유다.
 * ServiceType 에 REFUEL 을 하나 더하는 방식으로는 liters 를 담을 자리가 없다.
 */
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

    /** 주유 시점의 차량 주행거리. 직전 기록과의 차이가 곧 "그동안 달린 거리"다. */
    @Column(nullable = false)
    private int odometer;

    /**
     * 주유량(L). 32.45 같은 값이라 int 로는 담을 수 없다.
     *
     * <p>double 을 쓰지 않는 이유는 2진 부동소수라 0.1 + 0.2 != 0.3 이 되기 때문이다.
     * 리터를 합산해 평균 연비를 내는 순간 그 오차가 쌓인다. BigDecimal 은 10진 그대로 다룬다.
     * precision 6 / scale 2 = 최대 9999.99L — 승용차 한 번 주유로는 절대 넘지 않는다.
     */
    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal liters;

    /**
     * 총 결제 금액(원).
     *
     * <p>리터당 단가를 저장하지 않는 이유: 단가 × 리터는 반올림 때문에 영수증 총액과 어긋난다.
     * 실제로 나간 돈은 총액이므로 그쪽을 저장하고, 단가는 필요할 때 나눠서 보여준다.
     * 표시용 단가가 소수점에서 1원쯤 달라지는 건 무해하지만, 지출 합계가 틀리면 안 된다.
     */
    @Column(name = "total_cost", nullable = false)
    private int totalCost;

    @Column(length = 255)
    private String memo;

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
}
