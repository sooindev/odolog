package com.odolog.app.common.validation.limit;

/**
 * 입력값 상한 모음. 여러 애노테이션에 흩어진 숫자 방지
 * 목적은 되돌릴 수 없는 손상 방지. 차량 주행거리는 최댓값을 붙잡아 둠
 */
public final class InputLimits {

    /** 주행거리 상한(km). 정상 입력은 통과, 자리수 실수는 차단 */
    public static final long MAX_ODOMETER = 2_000_000L;

    /** 금액 상한(원) */
    public static final long MAX_AMOUNT = 100_000_000L;

    private InputLimits() {
    }
}
