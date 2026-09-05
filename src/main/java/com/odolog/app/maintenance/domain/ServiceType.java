package com.odolog.app.maintenance.domain;

public enum ServiceType {
    ENGINE_OIL(5000, 6),
    TIRE(10000, 24),
    BRAKE_PAD(20000, 24),
    BATTERY(30000, 36),
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
