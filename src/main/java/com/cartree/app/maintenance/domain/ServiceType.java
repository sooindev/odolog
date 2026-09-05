package com.cartree.app.maintenance.domain;

public enum ServiceType {
    ENGINE_OIL(5000),
    TIRE(10000),
    BRAKE_PAD(20000),
    BATTERY(30000),
    OTHER(null);

    private final Integer recommendedIntervalKm;

    ServiceType(Integer recommendedIntervalKm) {
        this.recommendedIntervalKm = recommendedIntervalKm;
    }

    public Integer getRecommendedIntervalKm() {
        return recommendedIntervalKm;
    }
}
