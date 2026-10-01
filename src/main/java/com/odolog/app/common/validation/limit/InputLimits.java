package com.odolog.app.common.validation.limit;

/**
 * 입력값 상한 모음. 여러 애노테이션에 흩어진 숫자 방지
 * 목적은 되돌릴 수 없는 손상 방지. 차량 주행거리는 최댓값을 붙잡아 둠
 */
public final class InputLimits {

    /** 주행거리 상한(km). 정상 입력은 통과, 자리수 실수는 차단 */
    public static final long MAX_ODOMETER = 2_000_000L;

    /** 금액 상한(통화의 최소 단위). 원화 1억, 달러 100만 */
    public static final long MAX_AMOUNT = 100_000_000L;

    /** 이메일 길이 상한. 가입·로그인·재설정 공용 */
    public static final int MAX_EMAIL_LENGTH = 100;

    /**
     * 이메일은 출력 가능한 ASCII 만. DB(unicode_ci)가 악센트·전각·제어 문자를 같은 글자로 봐서
     * 철자만 바꾼 주소가 같은 계정으로 로그인되면서 시도 횟수는 따로 세어짐
     */
    public static final String EMAIL_CHARS = "^[!-~]+$";

    private InputLimits() {
    }
}
