package com.odolog.app.maintenance.domain.entity;

import com.odolog.app.maintenance.domain.type.ServiceType;
import com.odolog.app.vehicle.domain.entity.Vehicle;
import com.odolog.app.common.domain.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "maintenance_records")
public class MaintenanceRecord extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "vehicle_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_maintenance_records_vehicle")
    )
    private Vehicle vehicle;

    // length 를 주지 않는다. Hibernate 6 은 MariaDB 에서 이 컬럼을 varchar 가 아니라
    /*
     * @JdbcTypeCode(VARCHAR) 가 핵심이다.
     *
     * 이게 없으면 Hibernate 6 이 MariaDB 에서 @Enumerated(STRING) 을 네이티브
     * enum('BATTERY','BRAKE_PAD',...) 컬럼으로 만든다. 그러면 자바 enum 에 값을 하나 추가할
     * 때마다 운영 DB 에 ALTER TABLE 이 필요하다 — ddl-auto: update 는 컬럼 타입을 바꿔 주지
     * 않으므로, 코드만 고치면 새 종류를 저장하는 순간 데이터 잘림 오류가 난다.
     * 게다가 odolog_test 는 create-drop 이라 테스트는 멀쩡히 통과한다.
     *
     * 2026-09-13 에 한 번 이 애노테이션을 검토하고 "잘 도는 컬럼을 바꾸려고 ALTER 를 하긴
     * 아깝다"며 반려했는데, 종류를 늘리면서 근거가 뒤집혔다: 이제 ALTER 는 어차피 필요하고,
     * varchar 로 바꿔 두면 앞으로 종류를 몇 개를 더 넣든 다시는 필요 없다.
     *
     * length 30 — 지금 가장 긴 이름이 TRANSMISSION_FLUID(18자)다.
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private ServiceType type;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private int cost;

    @Column(name = "service_odometer", nullable = false)
    private int serviceOdometer;

    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;


    protected MaintenanceRecord() {
    }

    public MaintenanceRecord(Vehicle vehicle, ServiceType type, String description,
                              int cost, int serviceOdometer, LocalDate serviceDate) {
        this.vehicle = vehicle;
        this.type = type;
        this.description = description;
        this.cost = cost;
        this.serviceOdometer = serviceOdometer;
        this.serviceDate = serviceDate;
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

    public String getDescription() {
        return description;
    }

    public int getCost() {
        return cost;
    }

    public int getServiceOdometer() {
        return serviceOdometer;
    }

    public LocalDate getServiceDate() {
        return serviceDate;
    }


    public void changeType(ServiceType type) {
        this.type = type;
    }

    public void changeDescription(String description) {
        this.description = description;
    }

    public void changeCost(int cost) {
        this.cost = cost;
    }

    public void changeServiceOdometer(int serviceOdometer) {
        this.serviceOdometer = serviceOdometer;
    }

    public void changeServiceDate(LocalDate serviceDate) {
        this.serviceDate = serviceDate;
    }
}
