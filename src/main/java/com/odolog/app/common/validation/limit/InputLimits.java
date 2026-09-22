package com.odolog.app.common.validation.limit;

/**
 * 입력값 상한. 애노테이션에 숫자를 직접 적지 않고 여기서 가져다 쓴다
 *
 * 한 곳에 모으는 이유: 같은 뜻의 상한이 주행거리 4곳·금액 4곳에 흩어지는데,
 * 숫자를 직접 적으면 나중에 한 곳만 고치게 된다
 *
 * 상한을 두는 목적은 "말이 되는 값인가" 가 아니라 <되돌릴 수 없게 망가지는 것을 막는 것>이다.
 * 주행거리는 Vehicle.liftOdometerTo 가 "지금까지 기록된 최댓값" 을 잡아 두기 때문에,
 * 자리수를 크게 잘못 넣으면 그 뒤 모든 폼이 그 값을 기준으로 말하기 시작한다
 */
public final class InputLimits {

    /**
     * 주행거리 상한(km)
     * 실제 차량이 도달할 수 없는 선. 기네스 최고 기록이 약 500만 km 이지만 그건 한 대의 이야기고,
     * 200만이면 정상적인 입력은 전부 통과하면서 자리수 실수는 걸린다
     */
    public static final long MAX_ODOMETER = 2_000_000L;

    /**
     * 금액 상한(원)
     * 정비 한 건이든 주유 한 번이든 1억을 넘지 않는다. 엔진 교체도 천만 원대다
     */
    public static final long MAX_AMOUNT = 100_000_000L;

    private InputLimits() {
    }
}
