import { describe, expect, it } from 'vitest'

import { cn, TYPE_SCALE } from '@/shared/ui/cn'

describe('프로젝트 cn', () => {
  it('크기 토큰과 색 토큰이 함께 남는다', () => {
    expect(cn('text-caption', 'text-strong')).toBe('text-caption text-strong')
    expect(cn('text-section', 'text-muted-foreground')).toBe('text-section text-muted-foreground')
  })

  it('크기끼리는 뒤엣것만 남는다', () => {
    expect(cn('text-[0.875rem]', 'text-caption')).toBe('text-caption')
    expect(cn('text-caption', 'text-body')).toBe('text-body')
    expect(cn('text-sm', 'text-unit')).toBe('text-unit')
    expect(cn('md:text-[0.9375rem]', 'md:text-caption')).toBe('md:text-caption')
  })

  it('색끼리는 뒤엣것만 남는다', () => {
    expect(cn('text-muted-foreground', 'text-strong')).toBe('text-strong')
  })

  // index.css 에 토큰을 더하면 cn.ts 의 TYPE_SCALE 에도. 빠지면 그 토큰만 색으로 추정됨
  it.each(TYPE_SCALE)('text-%s 를 크기로 인식한다', (token) => {
    expect(cn(`text-${token}`, 'text-strong')).toBe(`text-${token} text-strong`)
    expect(cn('text-[1px]', `text-${token}`)).toBe(`text-${token}`)
  })
})
