package com.odolog.app.maintenance.domain.entity;

import com.odolog.app.common.domain.PublicId;
import com.odolog.app.maintenance.domain.ServiceType;
import com.odolog.app.vehicle.Vehicle;
import com.odolog.app.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.hibernate.annotations.ColumnDefault;
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
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;

@Entity
@Table(
        name = "maintenance_records",
        uniqueConstraints = @UniqueConstraint(name = "uk_maintenance_records_public_id", columnNames = "public_id"))
public class MaintenanceRecord extends BaseTimeEntity {

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
            foreignKey = @ForeignKey(name = "fk_maintenance_records_vehicle")
    )
    private Vehicle vehicle;

    // @JdbcTypeCode(VARCHAR) 필수. 없으면 MariaDB 네이티브 enum 컬럼 생성
    // 네이티브 enum 은 종류 추가 시 운영 DB 만 데이터 잘림
    // length 30: 최장 이름 TRANSMISSION_FLUID(18자) 기준 여유
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private ServiceType type;

    @Column(length = 255)
    private String description;

    /**
     * 통화의 최소 단위(원·센트)
     * 비울 수 있음. 타던 차를 등록하며 "언제 갈았는지만 기억" 하는 경우. 0 은 "0원" 이라는 다른 뜻
     */
    @Column
    private Integer cost;

    /** ISO 4217. 기록마다 저장, 사용자 설정을 바꿔도 옛 기록의 뜻 유지 */
    @ColumnDefault("'KRW'")
    @Column(nullable = false, length = 3)
    private String currency;

    /** 비울 수 있음. 비면 다음 정비는 날짜 기준만 */
    @Column(name = "service_odometer")
    private Integer serviceOdometer;

    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;


    protected MaintenanceRecord() {
    }

    public MaintenanceRecord(Vehicle vehicle, ServiceType type, String description,
                              Integer cost, String currency, Integer serviceOdometer, LocalDate serviceDate) {
        this.publicId = PublicId.generate();
        this.vehicle = vehicle;
        this.type = type;
        this.description = description;
        this.cost = cost;
        this.currency = currency;
        this.serviceOdometer = serviceOdometer;
        this.serviceDate = serviceDate;
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

    public String getCurrency() {
        return currency;
    }

    public ServiceType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public Integer getCost() {
        return cost;
    }

    /** 합계용 금액. 안 적은 기록은 0. 한 건 표시에는 getCost() 의 null 그대로 */
    public int costOrZero() {
        return cost == null ? 0 : cost;
    }

    public Integer getServiceOdometer() {
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

    public void changeCost(Integer cost) {
        this.cost = cost;
    }

    public void changeServiceOdometer(Integer serviceOdometer) {
        this.serviceOdometer = serviceOdometer;
    }

    public void changeServiceDate(LocalDate serviceDate) {
        this.serviceDate = serviceDate;
    }
}
