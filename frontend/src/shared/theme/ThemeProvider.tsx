import { useCallback, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { flushSync } from 'react-dom'

import { THEME_STORAGE_KEY, ThemeContext } from '@/shared/theme/ThemeContext'
import type { Theme } from '@/shared/theme/ThemeContext'

const DARK_QUERY = '(prefers-color-scheme: dark)'

/** 사파리 사생활 보호 모드처럼 localStorage 접근 자체가 예외를 던지는 환경이 있다. */
function readStoredTheme(): Theme {
  try {
    const stored = localStorage.getItem(THEME_STORAGE_KEY)
    if (stored === 'light' || stored === 'dark' || stored === 'system') {
      return stored
    }
  } catch {
    // 무시하고 기본값으로 간다. 테마 때문에 앱이 안 뜨는 일은 없어야 한다.
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
 * <html> 에 클래스를 붙이거나 뗀다. 색은 전부 CSS 변수라 이 한 줄이면 화면 전체가 바뀐다.
 *
 * theme-color 메타 태그도 같이 갱신한다. 모바일 브라우저가 주소창을 이 색으로 칠하는데,
 * 안 바꾸면 화면은 다크인데 주소창만 밝은 회색으로 남아 위쪽이 잘려 보인다.
 */
function applyTheme(resolved: 'light' | 'dark') {
  document.documentElement.classList.toggle('dark', resolved === 'dark')

  const meta = document.querySelector('meta[name="theme-color"]')
  meta?.setAttribute('content', resolved === 'dark' ? '#000000' : '#f4f4f6')
}

/**
 * 누른 버튼의 중심에서 화면의 가장 먼 모서리까지의 거리.
 * 이 값을 원의 최종 반지름으로 써야 화면 구석까지 빠짐없이 덮인다.
 * 대충 큰 수(예: 150vmax)를 넣으면 원이 화면을 벗어난 뒤에도 애니메이션이 계속 돌아서
 * 끝부분이 멈춘 것처럼 보인다.
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
  // 초기값을 useState의 인자로 직접 계산한다. useEffect에서 맞추면 첫 렌더가 틀린 값으로
  // 한 번 돌아서, 세그먼트 컨트롤의 표시가 잠깐 엉뚱한 칸에 있다가 튄다.
  const [theme, setThemeState] = useState<Theme>(readStoredTheme)
  const [resolved, setResolved] = useState<'light' | 'dark'>(() => resolveTheme(readStoredTheme()))

  // 'system' 일 때 OS 설정이 바뀌면 즉시 따라간다. 앱을 껐다 켜야 반영되면 위임한 의미가 없다.
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

    // 정리 함수. 안 떼면 theme이 바뀔 때마다 리스너가 쌓인다.
    return () => media.removeEventListener('change', handleChange)
  }, [theme])

  const setTheme = useCallback((next: Theme, origin?: HTMLElement | null) => {
    try {
      localStorage.setItem(THEME_STORAGE_KEY, next)
    } catch {
      // 저장만 실패한다. 이번 세션 동안은 정상 동작한다.
    }

    const nextResolved = resolveTheme(next)

    function commit() {
      applyTheme(nextResolved)
      // flushSync: React 상태 변경을 지금 이 자리에서 DOM에 반영한다.
      // View Transition은 콜백이 끝난 직후의 화면을 찍기 때문에, 평소처럼 나중에 반영되면
      // 새 화면이 아니라 옛 화면을 두 번 찍게 된다.
      flushSync(() => {
        setThemeState(next)
        setResolved(nextResolved)
      })
    }

    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches

    // 이 API가 없는 브라우저(파이어폭스 일부 버전 등)에서는 그냥 즉시 바뀐다.
    // 기능이 사라지는 게 아니라 연출만 빠진다 — 이런 게 점진적 향상(progressive enhancement)이다.
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
