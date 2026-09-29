package com.odolog.app.user.domain.entity;

import com.odolog.app.common.domain.entity.BaseTimeEntity;
import com.odolog.app.common.exception.type.InvalidRequestException;
import com.odolog.app.common.money.CurrencyCode;
import com.odolog.app.user.domain.type.Language;
import com.odolog.app.user.domain.type.UnitSystem;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.ColumnDefault;

import java.time.ZoneId;


@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_users_email",
                columnNames = "email"
        )
)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 30)
    private String nickname;

    @Column(length = 20)
    private String phone;

    // 기존 행은 @ColumnDefault 로, 새 객체는 필드 초기값으로 채움
    // @Enumerated 대신 @Convert. @Enumerated 는 CHECK(값 목록)를 붙이고 ddl-auto 는 그 목록을 갱신하지 않음
    @Convert(converter = Language.Converter.class)
    @ColumnDefault("'KO'")
    @Column(nullable = false, length = 10)
    private Language language = Language.KO;

    /** IANA 이름(Asia/Seoul). "오늘" 판정 기준 */
    @ColumnDefault("'Asia/Seoul'")
    @Column(name = "time_zone", nullable = false, length = 64)
    private String timeZone = "Asia/Seoul";

    /** ISO 4217 코드(KRW) */
    @ColumnDefault("'KRW'")
    @Column(nullable = false, length = 3)
    private String currency = "KRW";

    @Convert(converter = UnitSystem.Converter.class)
    @ColumnDefault("'KM_PER_L'")
    @Column(name = "unit_system", nullable = false, length = 20)
    private UnitSystem unitSystem = UnitSystem.KM_PER_L;


    protected User() {
    }

    public User(String email, String password, String nickname, String phone) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.phone = phone;
    }


    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getNickname() {
        return nickname;
    }

    public String getPhone() {
        return phone;
    }

    public Language getLanguage() {
        return language;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public String getCurrency() {
        return currency;
    }

    public UnitSystem getUnitSystem() {
        return unitSystem;
    }


    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    public void changePhone(String phone) {
        this.phone = phone;
    }

    public void changeLanguage(Language language) {
        this.language = language;
    }

    /** 지역 이름만 허용. +09:00 같은 고정 오프셋은 서머타임 미반영이라 거절 */
    public void changeTimeZone(String timeZone) {
        if (!ZoneId.getAvailableZoneIds().contains(timeZone)) {
            throw new InvalidRequestException("지원하지 않는 시간대입니다: " + timeZone);
        }
        this.timeZone = timeZone;
    }

    public void changeCurrency(String currency) {
        if (!CurrencyCode.isKnown(currency)) {
            throw new InvalidRequestException("지원하지 않는 통화입니다: " + currency);
        }
        this.currency = currency;
    }

    public void changeUnitSystem(UnitSystem unitSystem) {
        this.unitSystem = unitSystem;
    }

    /** 암호화된 문자열만 받음. 엔티티의 스프링 시큐리티 의존 방지 */
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}
