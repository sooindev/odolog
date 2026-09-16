package com.odolog.app.maintenance.domain.entity;

import com.odolog.app.maintenance.domain.type.ServiceType;
import com.odolog.app.vehicle.domain.entity.Vehicle;
import com.odolog.app.common.domain.entity.BaseTimeEntity;
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
    // 네이티브 enum(...) 으로 만들어서 길이가 쓰이지 않는다.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
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
