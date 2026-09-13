import { createContext, useContext } from 'react'

/**
 * 'system' 은 "정하지 않음"이다. 'light'/'dark' 와 나란한 세 번째 선택지가 아니라,
 * OS 설정을 그대로 따르겠다는 위임이다. 그래서 저장된 값이 없을 때의 기본값이기도 하다.
 *
 * 이 구분이 없으면 낮에 라이트로 골라 둔 사용자가 밤에 OS가 다크로 바뀌어도
 * 계속 라이트를 보게 된다. 대부분의 사용자는 아무것도 고르지 않는 쪽이 정답이다.
 */
export type Theme = 'light' | 'dark' | 'system'

/**
 * localStorage 키. index.html 의 인라인 스크립트에도 같은 문자열이 박혀 있다
 * (React가 뜨기 전에 읽어야 해서 import 를 할 수 없다). 한쪽만 고치면 안 된다.
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

// 기본값을 null로 두면 <ThemeProvider>로 감싸는 걸 빠뜨렸을 때 조용히 동작하지 않고 바로 터진다.
export const ThemeContext = createContext<ThemeContextValue | null>(null)

export function useTheme() {
  const value = useContext(ThemeContext)

  if (value === null) {
    throw new Error('useTheme()는 <ThemeProvider> 안에서만 사용할 수 있습니다.')
  }

  return value
}
