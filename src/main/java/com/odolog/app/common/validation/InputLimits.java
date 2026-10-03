package com.odolog.app.common.validation;

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

    /** 비밀번호 글자 수 상한. 가입·변경과 같은 값. 확인용 칸(로그인·탈퇴)에도 적용 */
    public static final int MAX_PASSWORD_LENGTH = 100;

    /**
     * 요청 본문 상한(바이트). 검증 애노테이션은 JSON 을 다 읽은 뒤에 돌아서 그 전에 막음
     * 가져오기 파일(기록 2만 건, 들여쓰기 포함 약 7MB)이 들어가는 크기
     */
    public static final long MAX_BODY_BYTES = 10L * 1024 * 1024;

    private InputLimits() {
    }
}
