import { cn } from 'cn'
import { describe, expect, it } from 'vitest'

/*
 * cn(tailwind-merge 계열)은 커스텀 테마 이름을 크기로 인식하지 못하고 색으로 추정
 * text-red-500 과 모양이 같아 한 그룹으로 묶고 뒤엣것만 남김 — 크기가 조용히 사라짐
 * CardTitle 이 이 함정으로 8일간 17px·600 을 잃고 16px·400 으로 렌더됐음
 */

/** index.css 의 타입 스케일 토큰. 새 토큰을 만들면 여기에도 더할 것 */
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

/** 여는 괄호부터 짝이 맞는 닫는 괄호까지. 중첩 괄호를 그대로 지나감 */
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

/** sm: · hover: 같은 변형이 앞에 붙은 것도 같은 함정이라 경계에 : 를 포함 */
function mentions(text: string, token: string) {
  return new RegExp(`(^|[\\s'"\`:])${token}([\\s'"\`]|$)`).test(text)
}

// ?raw 로 소스를 문자열로 읽음. node:fs 를 쓰면 tsconfig 의 types 에 node 를 더해야 함
const sources = import.meta.glob('/src/**/*.{ts,tsx}', {
  query: '?raw',
  import: 'default',
  eager: true,
}) as Record<string, string>

function offenders(callee: string) {
  const found: string[] = []

  for (const [path, source] of Object.entries(sources)) {
    // 이 파일 자신이 토큰 이름을 들고 있음
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
    // 이 단언이 깨지면 tailwind-merge 가 토큰을 배웠다는 뜻 — 아래 두 금지가 풀린다
    expect(cn('text-caption', 'text-strong')).toBe('text-strong')
    expect(cn('text-section', 'text-strong')).toBe('text-strong')
  })

  it('임의 값과 기본 스케일은 크기로 인식되어 살아남는다', () => {
    // 그래서 card·label·button·state 네 파일이 임의 값을 쓴다
    expect(cn('text-[0.8125rem]', 'text-strong')).toBe('text-[0.8125rem] text-strong')
    expect(cn('text-sm', 'text-strong')).toBe('text-sm text-strong')
  })
})

describe('타입 스케일 토큰의 사용처', () => {
  it('cn() 인자에 들어가지 않는다', () => {
    expect(offenders('cn')).toEqual([])
  })

  it('cva() 인자에 들어가지 않는다', () => {
    // cva 는 이어 붙이기만 하므로 base 와 variant 가 둘 다 남고 CSS 배치 순서가 승자를 정함
    // 토큰 유틸리티가 임의 값보다 먼저 배치되어 size="lg" 버튼이 14px 로 작아졌던 자리
    expect(offenders('cva')).toEqual([])
  })
})
