package com.odolog.app.account.dto.response.restore;

/**
 * 복원 결과. 추가·병합·건너뜀 수
 * 말없이 건너뛰면 다시 누르게 되므로 개수 공개
 */
public record AccountRestoreResponse(
        int addedVehicles,
        int addedMaintenanceRecords,
        int addedFuelRecords,
        /** 같은 번호판이 있어 기록만 붙인 차량 수 */
        int mergedVehicles,
        /** 이미 같은 기록이 있어 건너뛴 수 */
        int skippedRecords,
        /**
         * 되살린 차량별 주기 수
         * 이미 설정된 종류는 유지
         */
        int addedServiceIntervals
) {
}
