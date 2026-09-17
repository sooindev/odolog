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

    /**
     * 연비를 여기서부터 다시 세라는 표시.
     *
     * <p>연비가 이상해졌을 때(주행거리를 잘못 넣었다거나, 계절·운전 습관이 바뀌었다거나)
     * 지금까지의 기록을 버리지 않고 기준만 다시 잡으려는 것이다.
     * <b>기록을 지우는 방식은 쓰지 않는다</b> — 그러면 유류비 통계까지 함께 사라진다.
     *
     * <p>기준점이 여럿이면 <b>가장 최근 것</b>이 이긴다. 한 번만 찍을 수 있게 막지 않는 이유는,
     * 여러 번 초기화하는 게 자연스러운 일이고 그때마다 옛 표시를 지우러 다닐 이유가 없어서다.
     */
    /*
     * @ColumnDefault 는 생성되는 DDL 에 `default false` 를 넣는다.
     * 이게 없으면 ddl-auto: update 가 기존 행이 있는 테이블에 NOT NULL 컬럼을 default 없이
     * 붙이게 되고, 옛 행에 무엇이 들어갈지는 DB 구현에 달린다. 기본값을 코드에 적어 둔다.
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
