import { createContext, useContext } from 'react'

/**
 * 'system' 은 세 번째 색이 아니라 "정하지 않음"이고, 저장된 값이 없을 때의 기본값이다.
 * 이게 없으면 낮에 라이트를 고른 사용자가 밤에 OS 가 바뀌어도 계속 라이트를 본다.
 */
export type Theme = 'light' | 'dark' | 'system'

/**
 * localStorage 키. index.html 인라인 스크립트에도 같은 문자열이 있다.
 * React 가 뜨기 전에 읽어야 해서 import 를 쓸 수 없다. 한쪽만 고치면 안 된다.
 */
export const THEME_STORAGE_KEY = 'odolog-theme'

export interface ThemeContextValue {
  /** 사용자가 고른 값. 'system' 이면 OS를 따른다. */
  theme: Theme
  /** 실제로 화면에 적용된 값. 'system' 일 때 지금 어느 쪽인지 알려면 이걸 본다. */
  resolved: 'light' | 'dark'
  /**
   * origin: 누른 버튼. 그 자리에서 원형으로 테마가 번지게 하는 데 쓴다.
   * 안 넘기면 화면 가운데에서 번진다.
   */
  setTheme: (theme: Theme, origin?: HTMLElement | null) => void
}

// 기본값이 null 이라 <ThemeProvider> 를 빠뜨리면 조용히 동작하지 않고 바로 터진다.
export const ThemeContext = createContext<ThemeContextValue | null>(null)

export function useTheme() {
  const value = useContext(ThemeContext)

  if (value === null) {
    throw new Error('useTheme()는 <ThemeProvider> 안에서만 사용할 수 있습니다.')
  }

  return value
}
