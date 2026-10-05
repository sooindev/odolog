package com.odolog.app.user.domain;

import jakarta.persistence.AttributeConverter;

/**
 * 거리·부피·연비 표기의 나라별 조합
 * 표시 전용. 저장은 언제나 km·L, 변환은 화면
 */
public enum UnitSystem {

    /** km · L · km/L (한국) */
    KM_PER_L,

    /** km · L · L/100km (캐나다·호주·유럽) */
    L_PER_100KM,

    /** mi · US gal · MPG (미국) */
    MPG_US,

    /** mi · L · 영국 갤런 MPG (영국. 주유는 리터, 연비는 영국 갤런) */
    MPG_UK;

    /** 이름 그대로 문자열 저장. CHECK 제약 없는 varchar 를 얻기 위한 변환기 */
    public static class Converter implements AttributeConverter<UnitSystem, String> {

        @Override
        public String convertToDatabaseColumn(UnitSystem value) {
            return value == null ? null : value.name();
        }

        @Override
        public UnitSystem convertToEntityAttribute(String value) {
            return value == null ? null : UnitSystem.valueOf(value);
        }
    }
}
