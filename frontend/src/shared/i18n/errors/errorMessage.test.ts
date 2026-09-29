import { describe, expect, it } from 'vitest'

import { ApiError, NETWORK_ERROR_STATUS } from '@/shared/api/client/client'
import { ERROR_CODES } from '@/shared/api/types/types'
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

describe('번역 누락', () => {
  it('서버의 모든 오류 코드에 두 언어 문구가 있다', () => {
    // 빠지면 그 오류만 대비 문구로 떨어져 무엇이 잘못됐는지 말하지 못함
    for (const code of ERROR_CODES) {
      expect(ko.errors.codes[code], code).toBeDefined()
      expect(en.errors.codes[code], code).toBeDefined()
    }
  })
})
