package com.odolog.app.common.money;

import java.util.Currency;

/** ISO 4217 통화 코드 판정. 사용자 설정·가져오기 공용 */
public final class CurrencyCode {

    /** 통화 칸이 생기기 전의 기록·백업 파일. 그때까지 전부 원화 */
    public static final String LEGACY = "KRW";

    private CurrencyCode() {
    }

    /** JDK 목록 기준. 모양만 맞는 "XYZ" 는 거절 */
    public static boolean isKnown(String code) {
        return code != null && Currency.getAvailableCurrencies().stream()
                .anyMatch(currency -> currency.getCurrencyCode().equals(code));
    }
}
