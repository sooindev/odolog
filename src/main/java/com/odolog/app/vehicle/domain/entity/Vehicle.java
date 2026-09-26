package com.odolog.app.vehicle.domain.entity;

import com.odolog.app.common.exception.type.ConflictException;
import com.odolog.app.user.domain.entity.User;
import com.odolog.app.common.domain.entity.BaseTimeEntity;
import com.odolog.app.common.domain.identifier.PublicId;
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


@Entity
@Table(
        name = "vehicles",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_vehicles_user_plate_number",
                        columnNames = {"user_id", "plate_number"}),
                @UniqueConstraint(
                        name = "uk_vehicles_public_id",
                        columnNames = "public_id")
        }
)
public class Vehicle extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** URL·API 용 공개 id(규칙 9-1) */
    @Column(name = "public_id", nullable = false, updatable = false, length = PublicId.LENGTH)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_vehicles_user")
    )
    private User owner;

    @Column(name = "plate_number", nullable = false, length = 20)
    private String plateNumber;

    @Column(nullable = false, length = 50)
    private String manufacturer;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "model_year")
    private Integer modelYear;

    @Column(nullable = false)
    private int odometer;


    protected Vehicle() {
    }

    public Vehicle(User owner, String plateNumber, String manufacturer, String modelName, Integer modelYear) {
        this.publicId = PublicId.generate();
        this.owner = owner;
        this.plateNumber = plateNumber;
        this.manufacturer = manufacturer;
        this.modelName = modelName;
        this.modelYear = modelYear;
        this.odometer = 0;
    }


    /** 사용자가 직접 고치는 값. 감소 시 409 */
    public void updateOdometer(int odometer) {
        if (odometer < this.odometer) {
            throw new ConflictException("주행거리는 줄어들 수 없습니다.");
        }
        this.odometer = odometer;
    }

    /**
     * 정정. 감소도 반영
     * 계기판 교체·자리수 오타용. force 로만 도달
     */
    public void correctOdometer(int odometer) {
        this.odometer = odometer;
    }

    /**
     * 기록에 따라 올리기. 작으면 무시
     * 과거 기록 입력은 정상 사용이라 예외 없음
     */
    public void liftOdometerTo(int odometer) {
        if (odometer > this.odometer) {
            this.odometer = odometer;
        }
    }

    // 번호판 중복 검사는 서비스 담당
    public void changePlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
    }

    public void changeManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public void changeModelName(String modelName) {
        this.modelName = modelName;
    }

    public void changeModelYear(Integer modelYear) {
        this.modelYear = modelYear;
    }

    public Long getId() {
        return id;
    }

    public String getPublicId() {
        return publicId;
    }

    public User getOwner() {
        return owner;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getModelName() {
        return modelName;
    }

    public Integer getModelYear() {
        return modelYear;
    }

    public int getOdometer() {
        return odometer;
    }

}
