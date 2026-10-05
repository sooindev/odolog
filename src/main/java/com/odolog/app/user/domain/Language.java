package com.odolog.app.user.domain;

import jakarta.persistence.AttributeConverter;

/**
 * 화면·메일 언어
 * 지원하는 언어만 값으로 존재. 문자열이면 "fr" 도 저장됨
 */
public enum Language {

    KO,
    EN;

    /** 이름 그대로 문자열 저장. CHECK 제약 없는 varchar 를 얻기 위한 변환기 */
    public static class Converter implements AttributeConverter<Language, String> {

        @Override
        public String convertToDatabaseColumn(Language value) {
            return value == null ? null : value.name();
        }

        @Override
        public Language convertToEntityAttribute(String value) {
            return value == null ? null : Language.valueOf(value);
        }
    }
}
