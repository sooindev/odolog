/** 백엔드 account DTO 대응(/v3/api-docs). 백엔드 변경 시 함께 수정 */

/** 계정 기록 전체. 백업용이라 계산값(연비·단가) 없음 */
export interface AccountExport {
  exportedAt: string
  user: {
    email: string
    nickname: string
    createdAt: string | null
  }
  vehicles: {
    plateNumber: string
    manufacturer: string
    modelName: string
    modelYear: number | null
    odometer: number
    createdAt: string | null
    maintenanceRecords: unknown[]
    fuelRecords: unknown[]
  }[]
}

/** 복원 결과. 건너뛴 수까지 공개 */
export interface AccountRestoreResult {
  addedVehicles: number
  addedMaintenanceRecords: number
  addedFuelRecords: number
  /** 같은 번호판이 있어 기록만 붙인 차량 수 */
  mergedVehicles: number
  /** 이미 같은 기록이 있어 건너뛴 수 */
  skippedRecords: number
  /** 되살린 차량별 주기 수 */
  addedServiceIntervals: number
}
