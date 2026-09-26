import { createContext, useContext } from 'react'

/** system = 정하지 않음. 저장값이 없을 때의 기본값, OS 변경 추적 */
export type Theme = 'light' | 'dark' | 'system'

/** localStorage 키. index.html 인라인 스크립트에 같은 문자열(함께 수정) */
export const THEME_STORAGE_KEY = 'odolog-theme'

export interface ThemeContextValue {
  /** 사용자가 고른 값 */
  theme: Theme
  /** 실제 적용된 값 */
  resolved: 'light' | 'dark'
  /** origin: 원형 전환의 시작점. 생략 시 화면 가운데 */
  setTheme: (theme: Theme, origin?: HTMLElement | null) => void
}

// 기본값 null. Provider 누락 시 즉시 오류
export const ThemeContext = createContext<ThemeContextValue | null>(null)

export function useTheme() {
  const value = useContext(ThemeContext)

  if (value === null) {
    throw new Error('useTheme()는 <ThemeProvider> 안에서만 사용할 수 있습니다.')
  }

  return value
}
