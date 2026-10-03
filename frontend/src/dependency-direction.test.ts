import { describe, expect, it } from 'vitest'

// 층 사이 의존 방향 고정. 소스의 import 를 훑음
// app → features → shared. 기능 사이는 아래 목록만

/** 기능 → import 해도 되는 다른 기능 */
const FEATURE_DEPENDENCIES: Record<string, string[]> = {
  // 백엔드 account → user 와 같은 방향. 프로필 화면이 계정 구역을 얹음
  account: ['auth'],
  auth: [],
  // 차량 상세가 정비·주유를 얹음
  vehicles: ['maintenance', 'fuel'],
  maintenance: [],
  fuel: [],
}

// ?raw 로 소스 읽기. node:fs 는 tsconfig 변경 필요
const sources = import.meta.glob('/src/**/*.{ts,tsx}', {
  query: '?raw',
  import: 'default',
  eager: true,
}) as Record<string, string>

// 따옴표 둘 다. shadcn 이 복사해 넣는 파일은 큰따옴표
const IMPORT = /(?:from|import)\s*\(?\s*['"]@\/([^'"]+)['"]/g

/** src 바로 아래에 둘 수 있는 것. 층 밖의 진입점 */
const ROOT_FILES = ['main.tsx', 'env.d.ts', 'index.css', 'dependency-direction.test.ts']

/** 'features/fuel/...' → 'features/fuel', 'shared/...' → 'shared' */
function layerOf(path: string) {
  const [top, name] = path.split('/')
  return top === 'features' ? `features/${name}` : top
}

function allowed(from: string, to: string) {
  // main.tsx 진입점은 층 밖
  if (ROOT_FILES.includes(from)) return true
  if (from === to) return true
  if (to === 'shared') return true
  if (from === 'shared') return false
  if (from === 'app') return to.startsWith('features/')
  if (from.startsWith('features/') && to.startsWith('features/')) {
    const own = from.slice('features/'.length)
    return FEATURE_DEPENDENCIES[own]?.includes(to.slice('features/'.length)) ?? false
  }
  return false
}

function violations() {
  const found: string[] = []

  for (const [path, source] of Object.entries(sources)) {
    const from = layerOf(path.replace('/src/', ''))
    for (const match of source.matchAll(IMPORT)) {
      const to = layerOf(match[1])
      if (!allowed(from, to)) found.push(`${path} → ${to}`)
    }
  }

  return found
}

describe('의존 방향', () => {
  it('허용된 방향으로만 import 한다', () => {
    expect(violations()).toEqual([])
  })

  it('모든 기능이 목록에 등록돼 있다', () => {
    const features = new Set(
      Object.keys(sources)
        .filter((path) => path.startsWith('/src/features/'))
        .map((path) => path.split('/')[3]),
    )
    expect([...features].sort()).toEqual(Object.keys(FEATURE_DEPENDENCIES).sort())
  })

  it('상대 경로로 위층을 거슬러 import 하지 않는다', () => {
    // '../' 는 이 검사가 방향을 읽지 못함
    const found = Object.entries(sources)
      .filter(([path, source]) => !path.includes('.test.') && /from\s+['"]\.\.\//.test(source))
      .map(([path]) => path)
    expect(found).toEqual([])
  })

  it('src 바로 아래에는 정해 둔 층과 진입점만 있다 — 새 층은 이 검사부터 고친다', () => {
    const tops = new Set(Object.keys(sources).map((path) => path.split('/')[2]))
    const unknown = [...tops].filter(
      (top) => !['app', 'features', 'shared'].includes(top) && !ROOT_FILES.includes(top),
    )
    expect(unknown).toEqual([])
  })
})
