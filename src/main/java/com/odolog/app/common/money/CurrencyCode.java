package com.odolog.app.common.money;

import java.util.Currency;
import java.util.Set;
import java.util.stream.Collectors;

/** ISO 4217 통화 코드 판정. 사용자 설정·가져오기 공용 */
public final class CurrencyCode {

    /** 통화 칸이 생기기 전의 기록·백업 파일. 그때까지 전부 원화 */
    public static final String LEGACY = "KRW";

    /** 한 번만 읽음. 가져오기에서 기록마다 300여 개를 훑지 않게 */
    private static final Set<String> KNOWN = Currency.getAvailableCurrencies().stream()
            .map(Currency::getCurrencyCode)
            .collect(Collectors.toUnmodifiableSet());

    private CurrencyCode() {
    }

    /** JDK 목록 기준. 모양만 맞는 "XYZ" 는 거절 */
    public static boolean isKnown(String code) {
        return code != null && KNOWN.contains(code);
    }
}
