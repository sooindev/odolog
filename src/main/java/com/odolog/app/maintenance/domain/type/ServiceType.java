package com.odolog.app.maintenance.domain.type;

/**
 * 정비 종류와 권장 주기.
 *
 * <p>주기는 <b>주행거리(km)와 개월 중 한쪽만 있을 수 있다.</b> 와이퍼는 얼마나 달렸느냐가 아니라
 * 고무가 굳는 시간 문제라 개월만 있고, 타이밍 벨트는 반대로 주행거리만 본다.
 * 계산하는 쪽이 둘을 따로 다루므로 한쪽이 null 이어도 나머지는 정상적으로 나온다.
 *
 * <p><b>순서는 알파벳순이 아니라 정비 부위별로 묶어 뒀다.</b> 화면의 선택 목록이 이 순서를
 * 그대로 따라가는데, 사람이 고를 때는 "엔진 계열 / 제동 계열 / 타이어 계열" 로 찾지
 * 이름의 첫 글자로 찾지 않는다. DB 에는 이름으로 저장되므로(@Enumerated STRING)
 * 순서를 바꿔도 저장된 데이터는 안전하다 — ORDINAL 이었다면 여기서 데이터가 깨진다.
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
