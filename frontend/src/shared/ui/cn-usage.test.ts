import { cn } from 'cn'
import { describe, expect, it } from 'vitest'

// cn 은 커스텀 테마 크기 토큰을 색으로 추정해 뒤엣것만 남김
// cn()·cva() 인자에 타입 스케일 토큰이 없는지 소스 검사

/** index.css 의 타입 스케일 토큰. 새 토큰 추가 시 함께 갱신 */
const TYPE_SCALE = [
  'text-eyebrow',
  'text-title',
  'text-display',
  'text-figure',
  'text-section',
  'text-lede',
  'text-body',
  'text-caption',
  'text-headline',
  'text-unit',
  'text-axis',
]

/** 여는 괄호부터 짝이 맞는 닫는 괄호까지. 중첩 괄호 통과 */
function argumentsOf(source: string, callee: string) {
  const found: string[] = []
  const opening = new RegExp(`\\b${callee}\\(`, 'g')
  let match: RegExpExecArray | null

  while ((match = opening.exec(source)) !== null) {
    const start = match.index + match[0].length
    let depth = 1
    let cursor = start

    while (cursor < source.length && depth > 0) {
      if (source[cursor] === '(') depth += 1
      else if (source[cursor] === ')') depth -= 1
      cursor += 1
    }

    found.push(source.slice(start, cursor - 1))
  }

  return found
}

/** sm:·hover: 같은 변형 접두사 포함 */
function mentions(text: string, token: string) {
  return new RegExp(`(^|[\\s'"\`:])${token}([\\s'"\`]|$)`).test(text)
}

// ?raw 로 소스 읽기. node:fs 는 tsconfig 변경 필요
const sources = import.meta.glob('/src/**/*.{ts,tsx}', {
  query: '?raw',
  import: 'default',
  eager: true,
}) as Record<string, string>

function offenders(callee: string) {
  const found: string[] = []

  for (const [path, source] of Object.entries(sources)) {
    // 이 파일은 토큰 이름을 가진 자신이라 제외
    if (path.includes('.test.')) continue

    for (const args of argumentsOf(source, callee)) {
      for (const token of TYPE_SCALE) {
        if (mentions(args, token)) found.push(`${path} — ${token}`)
      }
    }
  }

  return found
}

describe('cn 의 클래스 병합', () => {
  it('커스텀 테마 토큰은 색으로 오인되어 사라진다', () => {
    // 이 단언이 깨지면 tailwind-merge 의 토큰 인식 개선. 아래 금지 재검토
    expect(cn('text-caption', 'text-strong')).toBe('text-strong')
    expect(cn('text-section', 'text-strong')).toBe('text-strong')
  })

  it('임의 값과 기본 스케일은 크기로 인식되어 살아남는다', () => {
    // card·label·button·state 가 임의 값을 쓰는 이유
    expect(cn('text-[0.8125rem]', 'text-strong')).toBe('text-[0.8125rem] text-strong')
    expect(cn('text-sm', 'text-strong')).toBe('text-sm text-strong')
  })
})

describe('타입 스케일 토큰의 사용처', () => {
  it('cn() 인자에 들어가지 않는다', () => {
    expect(offenders('cn')).toEqual([])
  })

  it('cva() 인자에 들어가지 않는다', () => {
    // cva 는 base 와 variant 를 모두 남겨 CSS 배치 순서가 승자 결정. 토큰이 임의 값보다 먼저 배치
    expect(offenders('cva')).toEqual([])
  })
})
