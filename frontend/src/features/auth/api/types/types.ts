/**
 * 백엔드 com.odolog.app.user.dto 대응. 스펙은 /v3/api-docs
 * 자동 동기화가 아니므로 백엔드 DTO 를 고치면 여기도 함께
 */

export interface SignUpRequest {
  email: string
  password: string
  nickname: string
  /** 백엔드에 NotBlank 가 없는 선택 항목 */
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

/** 둘 다 필수. 부분 수정이 아니라 "현재 비밀번호를 대고 바꾸는" 한 동작 */
export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
}

/** 되돌릴 수 없는 동작이라 비밀번호로 본인 재확인 */
export interface WithdrawRequest {
  password: string
}

export interface UserResponse {
  id: number
  email: string
  nickname: string
  phone: string | null
}

/**
 * 계정의 기록 전부. 백업용이라 연비·단가 같은 계산값이 없다
 * 읽을 때 만들어지는 값이라 담아 두면 원본과 어긋날 수 있다
 */
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

/** 재설정 링크 요청. 가입 여부와 무관하게 응답이 같다 */
export interface PasswordResetRequest {
  email: string
}

export interface PasswordResetConfirmRequest {
  token: string
  newPassword: string
}

/**
 * 되돌려 넣은 결과
 * 개수를 받는 이유: 같은 파일을 두 번 넣으면 두 번째는 전부 건너뛴다.
 * 그때 아무 말도 없으면 "안 들어갔나?" 하고 또 누르게 된다
 */
export interface AccountRestoreResult {
  addedVehicles: number
  addedMaintenanceRecords: number
  addedFuelRecords: number
  /** 같은 번호판이 이미 있어 기록만 붙인 차량 수 */
  mergedVehicles: number
  /** 이미 같은 기록이 있어 건너뛴 수 */
  skippedRecords: number
}
