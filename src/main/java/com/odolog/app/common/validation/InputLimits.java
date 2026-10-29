package com.odolog.app.common.validation;

/**
 * 입력값 상한 모음. 여러 애노테이션에 흩어진 숫자 방지
 * 목적은 되돌릴 수 없는 손상 방지. 차량 주행거리는 최댓값을 붙잡아 둠
 */
public final class InputLimits {

    /** 주행거리 상한(km). 정상 입력은 통과, 자리수 실수는 차단 */
    public static final long MAX_ODOMETER = 2_000_000L;

    /**
     * 금액 상한(통화의 최소 단위). 원화 20억, 달러 2천만. 금액 칸이 INT 라 그 안의 깔끔한 값
     * 통화마다 한 숫자라 2자리 고액권(IDR 등)은 주 단위로 작아짐. Rp 2천만까지
     */
    public static final long MAX_AMOUNT = 2_000_000_000L;

    /** 주유량 상한(L). 칸이 DECIMAL(6,2). @DecimalMax 가 문자열을 받아 문자열 */
    public static final String MAX_LITERS = "9999.99";

    /** 기록 날짜 하한. 연식 하한과 같은 해. 연도 오타(0026)·음수 연도(DB 가 깨뜨려 저장) 차단 */
    public static final java.time.LocalDate MIN_RECORD_DATE = java.time.LocalDate.of(1900, 1, 1);

    /**
     * 한 줄 이름(닉네임). 제어 문자(Cc)·서식 문자(Cf, 글자 방향 뒤집기 포함)·줄·문단 구분자를 거부
     * 서식 문자 중 ZWJ(U+200D)는 가족 이모지 같은 합성 이모지에 쓰여 허용
     * \p{Cntrl} 은 ASCII 제어 문자만 뜻해 U+202E·U+2028 이 통과함
     * 짝 없는 서로게이트(Cs)도 거부. DB 가 '?' 로 바꿔 저장해 응답과 다음 조회의 값이 갈림
     */
    public static final String SINGLE_LINE_TEXT = "(?:[^\\p{Cc}\\p{Cf}\\p{Zl}\\p{Zp}\\p{Cs}]|\\u200D)*";

    /** 차량별 정비 주기 상한. 주기 변경·가져오기 공용, 화면 limits.ts 와 같은 값 */
    public static final int MAX_INTERVAL_KM = 500_000;
    public static final int MAX_INTERVAL_MONTHS = 120;

    /** 이메일 길이 상한. 가입·로그인·재설정 공용 */
    public static final int MAX_EMAIL_LENGTH = 100;

    /**
     * 이메일은 출력 가능한 ASCII 만. DB(unicode_ci)가 악센트·전각·제어 문자를 같은 글자로 봐서
     * 철자만 바꾼 주소가 같은 계정으로 로그인되면서 시도 횟수는 따로 세어짐
     */
    public static final String EMAIL_CHARS = "^[!-~]+$";

    /** 확인용 비밀번호 칸(로그인·탈퇴·현재 비밀번호)의 글자 수 상한. 새 비밀번호는 72바이트(@MaxBytes) */
    public static final int MAX_PASSWORD_LENGTH = 100;

    /**
     * 요청 본문 상한(바이트). 검증 애노테이션은 JSON 을 다 읽은 뒤에 돌아서 그 전에 막음
     * 가져오기 파일(기록 2만 건, 들여쓰기 포함 약 7MB)이 들어가는 크기
     */
    public static final long MAX_BODY_BYTES = 10L * 1024 * 1024;

    private InputLimits() {
    }
}
