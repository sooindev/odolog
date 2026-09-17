import { createContext, useContext } from 'react'

/**
 * system 은 세 번째 색이 아니라 "정하지 않음". 저장된 값이 없을 때의 기본값
 * 없으면 낮에 라이트를 고른 사용자가 밤에 OS 가 바뀌어도 계속 라이트를 봄
 */
export type Theme = 'light' | 'dark' | 'system'

/**
 * localStorage 키. index.html 인라인 스크립트에 같은 문자열이 중복
 * React 가 뜨기 전에 읽어야 해서 import 불가 — 한쪽만 고치면 안 됨
 */
export const THEME_STORAGE_KEY = 'odolog-theme'

export interface ThemeContextValue {
  /** 사용자가 고른 값. system 이면 OS 를 따름 */
  theme: Theme
  /** 실제 적용된 값. system 일 때 지금 어느 쪽인지 알려면 이쪽 */
  resolved: 'light' | 'dark'
  /** origin = 누른 버튼. 그 자리에서 원형으로 번짐. 생략하면 화면 가운데 */
  setTheme: (theme: Theme, origin?: HTMLElement | null) => void
}

// 기본값 null — Provider 를 빠뜨리면 조용히 넘어가지 않고 바로 터짐
export const ThemeContext = createContext<ThemeContextValue | null>(null)

export function useTheme() {
  const value = useContext(ThemeContext)

  if (value === null) {
    throw new Error('useTheme()는 <ThemeProvider> 안에서만 사용할 수 있습니다.')
  }

  return value
}
