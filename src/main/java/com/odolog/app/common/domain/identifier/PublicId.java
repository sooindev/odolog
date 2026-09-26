package com.odolog.app.common.domain.identifier;

import java.security.SecureRandom;

/**
 * URL·API 에 내보내는 무작위 식별자. 12자 영문·숫자(약 71비트)
 * 숫자 PK 를 대신 내보내는 이유 — 1,2,3… 은 서비스 규모와 등록 순서를 그대로 말한다
 */
public final class PublicId {

    public static final int LENGTH = 12;

    private static final char[] ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

    // Random 이 아니라 SecureRandom — 다음 값을 예측할 수 있으면 숨기는 의미가 없다
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
