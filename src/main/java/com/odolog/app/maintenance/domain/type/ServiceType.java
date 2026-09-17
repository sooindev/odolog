package com.odolog.app.maintenance.domain.type;

/**
 * 정비 종류와 권장 주기
 * 주기는 km · 개월 중 한쪽만 있을 수 있음 (와이퍼는 개월만, 타이밍 벨트는 km 만)
 * 선언 순서 = 화면 선택 목록 순서라 알파벳순이 아니라 부위별
 * 순서를 바꿔도 안전 — STRING 저장이라 (ORDINAL 이면 데이터가 깨짐)
 */
public enum ServiceType {

    // 엔진·구동 계열
    ENGINE_OIL(5000, 6),
    TRANSMISSION_FLUID(60000, 48),
    SPARK_PLUG(80000, 48),
    TIMING_BELT(100000, null),
    COOLANT(40000, 24),

    // 제동 계열
    BRAKE_PAD(20000, 24),
    BRAKE_FLUID(40000, 24),

    // 타이어·조향 계열
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
