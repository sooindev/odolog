package com.odolog.app.common.domain;

import java.security.SecureRandom;

/**
 * URL·API 용 무작위 식별자. 12자 영문·숫자(약 71비트)
 * 숫자 PK 대체. 서비스 규모·등록 순서 노출 방지
 */
public final class PublicId {

    public static final int LENGTH = 12;

    /** 대소문자 구분. 알파벳에 대소문자가 섞여 있어 unicode_ci 면 다른 id 가 같게 보임(V5) */
    public static final String COLUMN_DEFINITION = "varchar(12) character set utf8mb4 collate utf8mb4_bin";

    private static final char[] ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

    // SecureRandom 필수. 예측 가능한 값 방지
    private static final SecureRandom RANDOM = new SecureRandom();

    private PublicId() {
    }

    public static String generate() {
        char[] chars = new char[LENGTH];
        for (int i = 0; i < LENGTH; i++) {
            chars[i] = ALPHABET[RANDOM.nextInt(ALPHABET.length)];
        }
        return new String(chars);
    }
}
