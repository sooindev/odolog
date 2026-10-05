package com.odolog.app.account;

import com.odolog.app.account.dto.request.AccountRestoreRequest;
import com.odolog.app.common.validation.InputText;
import com.odolog.app.fuel.domain.FuelRecord;
import com.odolog.app.maintenance.domain.MaintenanceRecord;

import java.math.BigDecimal;

/**
 * 가져오기 중복 판정 열쇠. JSON 에 id 가 없어 내용으로 판정
 * 정비: 종류·날짜·주행거리·비용·통화·설명 / 주유: 날짜·주행거리·주유량·금액·통화
 * 날짜·주행거리만 보면 같은 날의 서로 다른 기록(와이퍼·경적 수리, 두 번 나눠 넣은 주유)을 하나로 봄
 */
final class RestoreKeys {

    private RestoreKeys() {
    }

    // 저장된 설명도 같은 규칙으로 정리. 정리 규칙 이전에 저장된 값과 파일 값의 비교 기준 통일
    static String of(MaintenanceRecord record) {
        return record.getType() + "|" + record.getServiceDate() + "|" + record.getServiceOdometer()
                + "|" + record.getCost() + "|" + record.getCurrency()
                + "|" + InputText.optional(record.getDescription());
    }

    static String of(AccountRestoreRequest.MaintenanceData record, String currency) {
        return record.type() + "|" + record.serviceDate() + "|" + record.serviceOdometer()
                + "|" + record.cost() + "|" + currency + "|" + InputText.optional(record.description());
    }

    static String of(FuelRecord record) {
        return record.getFueledAt() + "|" + record.getOdometer() + "|" + litersKey(record.getLiters())
                + "|" + record.getTotalCost() + "|" + record.getCurrency();
    }

    static String of(AccountRestoreRequest.FuelData record, String currency) {
        return record.fueledAt() + "|" + record.odometer() + "|" + litersKey(record.liters())
                + "|" + record.totalCost() + "|" + currency;
    }

    /** DB 는 32.40, 파일은 32.4 일 수 있어 자리수 맞춤 */
    private static String litersKey(BigDecimal liters) {
        return liters == null ? "null" : liters.stripTrailingZeros().toPlainString();
    }
}
