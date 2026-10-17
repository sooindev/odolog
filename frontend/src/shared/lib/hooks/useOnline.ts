import { onlineManager } from '@tanstack/react-query'
import { useSyncExternalStore } from 'react'

/**
 * 조회 캐시가 보는 연결 상태. 오프라인이면 조회가 실패하지 않고 멈춰 있다가 연결되면 이어짐
 * navigator.onLine 이 아니라 onlineManager 를 읽음. 멈춘 조회와 안내가 같은 판단을 따르게
 */
export function useOnline(): boolean {
  return useSyncExternalStore(
    (onChange) => onlineManager.subscribe(onChange),
    () => onlineManager.isOnline(),
  )
}
