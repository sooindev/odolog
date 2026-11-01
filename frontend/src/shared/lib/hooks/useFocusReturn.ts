import { useEffect, useRef } from 'react'
import type { RefObject } from 'react'

/**
 * 폼이 닫히며 포커스를 잃으면 연 요소로 되돌림. 연 요소가 없거나 사라졌으면 fallback
 * 다른 곳에 포커스가 있으면 건드리지 않음
 */
export function useFocusReturn(open: boolean, opener: HTMLElement | null, fallback: RefObject<HTMLElement | null>) {
  const wasOpen = useRef(open)

  useEffect(() => {
    // 폼 안에 있던 포커스는 폼이 사라지면 body 로 떨어짐
    if (wasOpen.current && !open && document.activeElement === document.body) {
      const target = opener?.isConnected ? opener : fallback.current
      target?.focus()
    }
    wasOpen.current = open
  }, [open, opener, fallback])
}
