package com.odolog.app.account.dto.response.restore;

/**
 * 무엇이 들어갔고 무엇이 건너뛰어졌는지
 *
 * 개수를 돌려주는 이유: 같은 파일을 두 번 넣으면 두 번째는 전부 건너뛴다.
 * 그때 아무 말도 없으면 "안 들어갔나?" 하고 또 누르게 된다 —
 * 평균에서 뺀 구간 수를 밝히는 것과 같은 이유다
 */
public record AccountRestoreResponse(
        int addedVehicles,
        int addedMaintenanceRecords,
        int addedFuelRecords,
        /** 같은 번호판이 이미 있어 기록만 붙인 차량 수 */
        int mergedVehicles,
        /** 이미 같은 기록이 있어 건너뛴 수 */
        int skippedRecords,
        /**
         * 되살린 차량별 주기 수
         * 이미 설정이 있는 종류는 건드리지 않는다 — 차량 정보를 안 덮어쓰는 것과 같은 이유로,
         * 파일이 옛날 것일 수 있는데 지금 설정을 밀어낼 이유가 없다
         */
        int addedServiceIntervals
) {
}
