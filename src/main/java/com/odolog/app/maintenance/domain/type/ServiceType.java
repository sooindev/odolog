package com.odolog.app.maintenance.domain.type;

/**
 * 정비 종류와 권장 주기(km·개월 중 한쪽만 있을 수 있음)
 * 선언 순서 = 화면 선택 목록 순서(부위별)
 * STRING 저장이라 순서 변경 안전
 */
public enum ServiceType {

    // 엔진·구동
    ENGINE_OIL(5000, 6),
    TRANSMISSION_FLUID(60000, 48),
    SPARK_PLUG(80000, 48),
    TIMING_BELT(100000, null),
    COOLANT(40000, 24),

    // 제동
    BRAKE_PAD(20000, 24),
    BRAKE_FLUID(40000, 24),

    // 타이어·조향
    TIRE(50000, 48),
    TIRE_ROTATION(10000, 6),
    WHEEL_ALIGNMENT(20000, 12),

    // 필터·소모품
    AIR_FILTER(20000, 12),
    CABIN_FILTER(15000, 12),
    BATTERY(30000, 36),
    WIPER(null, 12),

    OTHER(null, null);

    private final Integer recommendedIntervalKm;
    private final Integer recommendedIntervalMonths;

    ServiceType(Integer recommendedIntervalKm, Integer recommendedIntervalMonths) {
        this.recommendedIntervalKm = recommendedIntervalKm;
        this.recommendedIntervalMonths = recommendedIntervalMonths;
    }

    public Integer getRecommendedIntervalKm() {
        return recommendedIntervalKm;
    }

    public Integer getRecommendedIntervalMonths() {
        return recommendedIntervalMonths;
    }
}
