import { describe, expect, it } from 'vitest'

import { ApiError, NETWORK_ERROR_STATUS } from '@/shared/api/client'
import { ERROR_CODES } from '@/shared/api/types'
import { en } from '@/shared/i18n/messages/en'
import { ko } from '@/shared/i18n/messages/ko'

import { errorMessage } from './errorMessage'

describe('오류 문구', () => {
  it('코드로 언어별 문구를 고른다', () => {
    const error = new ApiError(409, '이미 등록하신 차량 번호입니다: 12가3456', { code: 'PLATE_DUPLICATE' })

    expect(errorMessage(error, ko, 'x')).toBe('이미 등록하신 차량 번호입니다.')
    expect(errorMessage(error, en, 'x')).toBe('You’ve already added a vehicle with this plate.')
  })

  it('칸 이름과 남은 분을 끼워 넣는다', () => {
    const invalid = new ApiError(400, 'nickname: …', { code: 'VALIDATION_FAILED', field: 'nickname' })
    const locked = new ApiError(429, '…', { code: 'TOO_MANY_LOGIN_ATTEMPTS', retryAfterMinutes: 1 })

    expect(errorMessage(invalid, ko, 'x')).toBe('닉네임 값을 확인해 주세요.')
    expect(errorMessage(locked, en, 'x')).toBe('Too many login attempts. Try again in 1 minute.')
  })

  it('서버에 닿지 못했으면 연결 문구, 코드가 없으면 기본 문구', () => {
    expect(errorMessage(new ApiError(NETWORK_ERROR_STATUS, 'network error'), en, 'x')).toBe(en.errors.network)
    // 서버 원문(한국어)을 영어 화면에 흘리지 않음
    expect(errorMessage(new ApiError(502, 'Bad Gateway'), en, 'Couldn’t save.')).toBe('Couldn’t save.')
    expect(errorMessage(new TypeError('boom'), ko, '저장에 실패했습니다.')).toBe('저장에 실패했습니다.')
  })
})

describe('모르는 경우', () => {
  it('사전에 없는 코드는 예외 대신 기본 문구', () => {
    // 새 백엔드 + 캐시된 옛 화면. 던지면 저장 버튼이 "저장 중…" 에 멈춤
    const unknown = new ApiError(400, '…', { code: 'SOMETHING_NEW' as never })

    expect(errorMessage(unknown, ko, '저장에 실패했습니다.')).toBe('저장에 실패했습니다.')
  })

  it('칸 이름이 없으면 주어 없는 문장 대신 기본 문구', () => {
    // JSON 자체가 깨지면 서버가 field 없이 MALFORMED_BODY
    const malformed = new ApiError(400, '…', { code: 'MALFORMED_BODY' })

    expect(errorMessage(malformed, en, 'Couldn’t save.')).toBe('Couldn’t save.')
  })
})

describe('번역 누락', () => {
  it('서버의 모든 오류 코드에 두 언어 문구가 있다', () => {
    // 빠지면 그 오류만 대비 문구로 떨어져 무엇이 잘못됐는지 말하지 못함
    for (const code of ERROR_CODES) {
      expect(ko.errors.codes[code], code).toBeDefined()
      expect(en.errors.codes[code], code).toBeDefined()
    }
  })

  it('가져오기의 중첩 경로는 마지막 조각으로, 끝내 모르는 칸은 기본 문구', () => {
    const nested = new ApiError(400, '…', {
      code: 'VALIDATION_FAILED',
      field: 'vehicles[0].fuelRecords[3].liters',
    })
    const unknownField = new ApiError(400, '…', { code: 'VALIDATION_FAILED', field: 'vehicles[0]' })

    expect(errorMessage(nested, ko, 'x')).toBe('주유량 값을 확인해 주세요.')
    // 영문 경로를 문장에 넣지 않음
    expect(errorMessage(unknownField, ko, '가져오지 못했습니다.')).toBe('가져오지 못했습니다.')
  })
})
