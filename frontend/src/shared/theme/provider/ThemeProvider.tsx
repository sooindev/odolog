import { useCallback, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { flushSync } from 'react-dom'

import { THEME_STORAGE_KEY, ThemeContext } from '@/shared/theme/context/ThemeContext'
import type { Theme } from '@/shared/theme/context/ThemeContext'

const DARK_QUERY = '(prefers-color-scheme: dark)'

/** localStorage 접근 자체가 예외인 환경 대비 */
function readStoredTheme(): Theme {
  try {
    const stored = localStorage.getItem(THEME_STORAGE_KEY)
    if (stored === 'light' || stored === 'dark' || stored === 'system') {
      return stored
    }
  } catch {
    // 읽기 실패 시 기본값
  }

  return 'system'
}

function resolveTheme(theme: Theme): 'light' | 'dark' {
  if (theme === 'system') {
    return window.matchMedia(DARK_QUERY).matches ? 'dark' : 'light'
  }

  return theme
}

/** html 클래스 토글. theme-color(모바일 주소창)도 함께 */
function applyTheme(resolved: 'light' | 'dark') {
  document.documentElement.classList.toggle('dark', resolved === 'dark')

  const meta = document.querySelector('meta[name="theme-color"]')
  meta?.setAttribute('content', resolved === 'dark' ? '#17171a' : '#f4f4f6')
}

/** 원형 전환 반지름 = 버튼 중심에서 가장 먼 모서리까지 거리 */
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
  // 초기값은 useState 인자에서 계산. 세그먼트 튐 방지
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

    // 리스너 누적 방지
    return () => media.removeEventListener('change', handleChange)
  }, [theme])

  const setTheme = useCallback((next: Theme, origin?: HTMLElement | null) => {
    try {
      localStorage.setItem(THEME_STORAGE_KEY, next)
    } catch {
      // 저장만 실패, 현재 세션은 정상
    }

    const nextResolved = resolveTheme(next)

    function commit() {
      applyTheme(nextResolved)
      // flushSync: View Transition 이 변경 후 화면을 찍도록
      flushSync(() => {
        setThemeState(next)
        setResolved(nextResolved)
      })
    }

    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches

    // 미지원 브라우저는 즉시 전환
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
