package com.odolog.app.common.validation;

import com.odolog.app.common.exception.ErrorCode;
import com.odolog.app.common.exception.type.InvalidRequestException;

/**
 * 입력 앞뒤 공백 정리. 서비스가 저장 직전 호출
 * DB 는 뒤 공백 무시, 자바 equals 는 구분. 그 차이로 생기는 자기 중복 409 방지
 */
public final class InputText {

    private InputText() {
    }

    /** 앞뒤 공백 제거. strip() 이 남기는 NBSP(U+00A0·U+2007·U+202F)·폭 없는 공백(U+200B)·BOM(U+FEFF)까지 */
    public static String strip(String value) {
        if (value == null) {
            return null;
        }
        int start = 0;
        int end = value.length();
        while (start < end && isBlank(value.charAt(start))) {
            start++;
        }
        while (end > start && isBlank(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(start, end);
    }

    /** 화면에 아무것도 안 보이는 글자. isSpaceChar 가 NBSP·전각 공백을, isWhitespace 가 탭·줄바꿈을 맡음 */
    private static boolean isBlank(char c) {
        return Character.isWhitespace(c) || Character.isSpaceChar(c) || c == '\u200B' || c == '\uFEFF';
    }

    /**
     * 글자로 세어지지만 화면에 아무것도 안 그려지는 것. 서식 문자(ZWJ 등)·이형 선택자·한글 채움 문자·점자 빈칸
     * 다른 글자와 섞이면 그대로 둠. 합성 이모지의 ZWJ 를 지우면 이모지가 쪼개짐
     */
    private static boolean isInvisible(int codePoint) {
        return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint)
                || Character.getType(codePoint) == Character.FORMAT
                || (codePoint >= 0xFE00 && codePoint <= 0xFE0F)
                || (codePoint >= 0xE0100 && codePoint <= 0xE01EF)
                || codePoint == 0x115F || codePoint == 0x1160 || codePoint == 0x3164 || codePoint == 0xFFA0
                || codePoint == 0x2800 || codePoint == 0x034F;
    }

    /**
     * 필수 입력용 strip. 지운 뒤 비거나 보이지 않는 글자만 남으면 400
     * @NotBlank(trim 기준)·\S 는 전각 공백·NBSP·한글 채움 문자를 글자로 보고 통과시킴. 그대로 두면 빈 칸으로 보이는 이름 저장
     */
    public static String required(String value, String field) {
        String stripped = strip(value);
        if (stripped != null && stripped.codePoints().allMatch(InputText::isInvisible)) {
            throw new InvalidRequestException(ErrorCode.VALIDATION_FAILED,
                    field + ": 공백만으로는 설정할 수 없습니다", field);
        }
        return stripped;
    }

    /**
     * 번호판용 required. 가운데 공백까지 지움
     * DB 는 가운데 공백을 다른 글자로 봐서 '33나 3333' 과 '33나3333' 이 다른 차로 들어감
     */
    public static String plateNumber(String value, String field) {
        String stripped = required(value, field);
        if (stripped == null) {
            return null;
        }
        StringBuilder text = new StringBuilder(stripped.length());
        for (int i = 0; i < stripped.length(); i++) {
            char c = stripped.charAt(i);
            if (!isBlank(c)) {
                text.append(c);
            }
        }
        return text.toString();
    }

    /**
     * 선택 입력용 strip. 비면 null(빈 문자열 저장 금지)
     * 짝 없는 서로게이트는 DB 가 저장하는 모양('?')으로. 비교 열쇠와 저장값을 같게
     */
    public static String optional(String value) {
        String stripped = strip(value);
        if (stripped == null || stripped.isEmpty()) {
            return null;
        }
        StringBuilder text = new StringBuilder(stripped.length());
        for (int i = 0; i < stripped.length(); i++) {
            char c = stripped.charAt(i);
            if (Character.isHighSurrogate(c) && i + 1 < stripped.length()
                    && Character.isLowSurrogate(stripped.charAt(i + 1))) {
                text.append(c).append(stripped.charAt(++i));
            } else {
                text.append(Character.isSurrogate(c) ? '?' : c);
            }
        }
        return text.toString();
    }
}
