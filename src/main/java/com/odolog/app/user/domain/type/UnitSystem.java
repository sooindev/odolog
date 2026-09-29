package com.odolog.app.user.domain.type;

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
    MPG_UK
}
