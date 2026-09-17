package com.odolog.app.vehicle.domain.entity;

import com.odolog.app.common.exception.type.ConflictException;
import com.odolog.app.user.domain.entity.User;
import com.odolog.app.common.domain.entity.BaseTimeEntity;
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
        uniqueConstraints = @UniqueConstraint(
                name = "uk_vehicles_user_plate_number",
                columnNames = {"user_id", "plate_number"}
        )
)
public class Vehicle extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
        this.owner = owner;
        this.plateNumber = plateNumber;
        this.manufacturer = manufacturer;
        this.modelName = modelName;
        this.modelYear = modelYear;
        this.odometer = 0;
    }


    /** 사용자가 "이 값으로 바꾸겠다"고 직접 누른 것. 줄어들면 잘못을 알려야 한다. */
    public void updateOdometer(int odometer) {
        if (odometer < this.odometer) {
            throw new ConflictException("주행거리는 줄어들 수 없습니다.");
        }
        this.odometer = odometer;
    }

    /**
     * 기록의 주행거리가 지금보다 크면 따라 올린다. 작거나 같으면 아무 일도 하지 않는다.
     *
     * <p><b>updateOdometer 와 나눠 둔 이유.</b> 저쪽은 사용자가 주행거리 자체를 고치는 동작이라
     * 줄어들면 예외로 막아야 한다. 이쪽은 정비·주유를 기록하다 <b>따라오는</b> 것이라
     * 과거 기록을 뒤늦게 넣는 경우가 정상이고, 거기서 예외가 나면 기록 자체가 막혀 버린다.
     * 같은 필드를 건드리지만 의도가 달라서 이름도 둘이다.
     */
    public void liftOdometerTo(int odometer) {
        if (odometer > this.odometer) {
            this.odometer = odometer;
        }
    }

    // 번호판 중복 검사는 여기서 못 한다. 다른 행을 봐야 하는 일이라 리포지토리가 필요하고,
    // 엔티티가 리포지토리를 알면 "자기 자신만 아는 객체" 라는 성질이 깨진다. 그래서 서비스가 맡는다.
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
