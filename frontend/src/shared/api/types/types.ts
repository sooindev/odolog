/** 기능에 속하지 않는 공용 타입. 기능별 DTO 는 각 feature 에 */

/** 백엔드 PageResponse */
export interface PageResponse<T> {
  items: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  hasNext: boolean
}

/** 백엔드 ErrorResponse */
export interface ErrorResponse {
  message: string
}
