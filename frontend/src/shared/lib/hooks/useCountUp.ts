import { useEffect, useRef, useState } from 'react'

/**
 * 직전 값에서 새 값으로 굴러가는 숫자. 첫 렌더는 정지
 * 지속 시간은 변화 폭에 비례
 * running: 움직이는 동안만 tabular-nums
 */
export function useCountUp(target: number): { value: number; running: boolean } {
  const [value, setValue] = useState(target)
  const [running, setRunning] = useState(false)

  // 직전 목표값
  const fromRef = useRef(target)
  // 현재 표시 중인 값. 도중 변경 시 출발점
  const shownRef = useRef(target)

  useEffect(() => {
    const from = fromRef.current
    if (from === target) {
      // 도중에 끊긴 연출이 현재 값에서 멈춘 경우. running 해제
      setRunning(false)
      return
    }

    // 1,000 당 약 0.1초, 0.45~1.4초 범위
    const distance = Math.abs(target - from)
    const duration = Math.min(1400, Math.max(450, (distance / 1000) * 100 + 400))

    const start = performance.now()
    let frame = 0

    function tick(now: number) {
      const progress = Math.min(1, (now - start) / duration)
      // ease-out quart
      const eased = 1 - (1 - progress) ** 4

      shownRef.current = Math.round(from + (target - from) * eased)
      setValue(shownRef.current)

      if (progress < 1) {
        frame = requestAnimationFrame(tick)
      } else {
        fromRef.current = target
        setRunning(false)
      }
    }

    // effect 안 동기 setState 회피. 첫 프레임에서 시작
    frame = requestAnimationFrame((now) => {
      // 연출을 끈 사용자에게는 결과만
      if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
        fromRef.current = target
        shownRef.current = target
        setValue(target)
        return
      }

      setRunning(true)
      tick(now)
    })

    return () => {
      cancelAnimationFrame(frame)
      // 다음 출발점은 화면에 보이던 값. 숫자 튐 방지
      fromRef.current = shownRef.current
    }
  }, [target])

  return { value, running }
}
