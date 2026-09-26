/** 백엔드 user DTO 대응(/v3/api-docs). 백엔드 변경 시 함께 수정 */

export interface SignUpRequest {
  email: string
  password: string
  nickname: string
  /** 선택 항목 */
  phone?: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface UpdateProfileRequest {
  nickname?: string
  phone?: string
}

/** 둘 다 필수 */
export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
}

/** 본인 재확인용 비밀번호 */
export interface WithdrawRequest {
  password: string
}

export interface UserResponse {
  email: string
  nickname: string
  phone: string | null
}

/** 계정 기록 전체. 백업용이라 계산값(연비·단가) 없음 */
export interface AccountExport {
  exportedAt: string
  user: {
    email: string
    nickname: string
    phone: string | null
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

/** 재설정 링크 요청. 가입 여부와 무관하게 같은 응답 */
export interface PasswordResetRequest {
  email: string
}

export interface PasswordResetConfirmRequest {
  token: string
  newPassword: string
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
