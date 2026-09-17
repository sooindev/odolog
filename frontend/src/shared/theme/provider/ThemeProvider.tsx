import { useCallback, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { flushSync } from 'react-dom'

import { THEME_STORAGE_KEY, ThemeContext } from '@/shared/theme/context/ThemeContext'
import type { Theme } from '@/shared/theme/context/ThemeContext'

const DARK_QUERY = '(prefers-color-scheme: dark)'

/** 사파리 사생활 보호 모드 등 localStorage 접근 자체가 예외인 환경 대비 */
function readStoredTheme(): Theme {
  try {
    const stored = localStorage.getItem(THEME_STORAGE_KEY)
    if (stored === 'light' || stored === 'dark' || stored === 'system') {
      return stored
    }
  } catch {
    // 무시하고 기본값으로. 테마 때문에 앱이 안 뜨면 안 됨
  }

  return 'system'
}

function resolveTheme(theme: Theme): 'light' | 'dark' {
  if (theme === 'system') {
    return window.matchMedia(DARK_QUERY).matches ? 'dark' : 'light'
  }

  return theme
}

/**
 * html 클래스 토글. 색이 전부 CSS 변수라 이것만으로 화면 전체가 바뀜
 * theme-color 는 모바일 주소창 — 안 바꾸면 거기만 반대 테마로 남음
 */
function applyTheme(resolved: 'light' | 'dark') {
  document.documentElement.classList.toggle('dark', resolved === 'dark')

  const meta = document.querySelector('meta[name="theme-color"]')
  meta?.setAttribute('content', resolved === 'dark' ? '#17171a' : '#f4f4f6')
}

/**
 * 버튼 중심에서 가장 먼 모서리까지의 거리 = 원의 반지름
 * 150vmax 같은 큰 수면 원이 화면을 벗어난 뒤에도 애니메이션이 돌아 끝부분이 멈춘 것처럼 보임
 */
function setRevealOrigin(origin: HTMLElement | null | undefined) {
  const style = document.documentElement.style

  if (!origin) {
    style.removeProperty('--vt-x')
    style.removeProperty('--vt-y')
    style.removeProperty('--vt-r')
    return
  }

  const box = origin.getBoundingClientRect()
  const x = box.left + box.width / 2
  const y = box.top + box.height / 2
  const radius = Math.hypot(Math.max(x, innerWidth - x), Math.max(y, innerHeight - y))

  style.setProperty('--vt-x', `${x}px`)
  style.setProperty('--vt-y', `${y}px`)
  style.setProperty('--vt-r', `${radius}px`)
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  // 초기값은 useState 인자에서 계산. useEffect 에서 맞추면 세그먼트 표시가 잠깐 엉뚱한 칸에 있다 튐
  const [theme, setThemeState] = useState<Theme>(readStoredTheme)
  const [resolved, setResolved] = useState<'light' | 'dark'>(() => resolveTheme(readStoredTheme()))

  // system 일 때 OS 설정 추적
  useEffect(() => {
    if (theme !== 'system') {
      return
    }

    const media = window.matchMedia(DARK_QUERY)

    function handleChange() {
      const next = media.matches ? 'dark' : 'light'
      applyTheme(next)
      setResolved(next)
    }

    media.addEventListener('change', handleChange)

    // 안 떼면 theme 이 바뀔 때마다 리스너가 쌓임
    return () => media.removeEventListener('change', handleChange)
  }, [theme])

  const setTheme = useCallback((next: Theme, origin?: HTMLElement | null) => {
    try {
      localStorage.setItem(THEME_STORAGE_KEY, next)
    } catch {
      // 저장만 실패. 이번 세션 동안은 정상 동작
    }

    const nextResolved = resolveTheme(next)

    function commit() {
      applyTheme(nextResolved)
      // View Transition 은 콜백 직후의 화면을 찍음. 평소 비동기 렌더면 DOM 이 아직 안 바뀌어
      // 옛 화면을 두 번 찍게 됨
      flushSync(() => {
        setThemeState(next)
        setResolved(nextResolved)
      })
    }

    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches

    // 미지원 브라우저는 즉시 전환. 기능이 아니라 연출만 빠지는 점진적 향상
    if (reduceMotion || typeof document.startViewTransition !== 'function') {
      commit()
      return
    }

    setRevealOrigin(origin)
    document.startViewTransition(commit)
  }, [])

  const value = useMemo(() => ({ theme, resolved, setTheme }), [theme, resolved, setTheme])

  return <ThemeContext value={value}>{children}</ThemeContext>
}
