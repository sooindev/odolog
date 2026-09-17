import { useEffect, useRef, useState } from 'react'

/**
 * 직전 값에서 새 값으로 굴러가는 숫자
 * 첫 렌더에서는 정지 — 0 부터 세면 값을 읽기까지 기다려야 함. 값이 실제로 바뀐 순간에만 동작
 * 지속 시간은 변화 폭에 비례. 10km 와 20,000km 가 같은 시간이면 작은 변화는 굼뜸
 * running 은 글자 폭용 — 매 프레임 폭이 다르면 떨려서 움직이는 동안만 tabular-nums
 */
export function useCountUp(target: number): { value: number; running: boolean } {
  const [value, setValue] = useState(target)
  const [running, setRunning] = useState(false)

  // 직전 값. 첫 렌더에서는 target 과 같아 무동작
  const fromRef = useRef(target)

  useEffect(() => {
    const from = fromRef.current
    if (from === target) {
      return
    }

    // 1,000 당 약 0.1초, 0.45~1.4초로 제한
    const distance = Math.abs(target - from)
    const duration = Math.min(1400, Math.max(450, (distance / 1000) * 100 + 400))

    const start = performance.now()
    let frame = 0

    function tick(now: number) {
      const progress = Math.min(1, (now - start) / duration)
      // ease-out quart. CSS 의 ease-apple 과 같은 성격
      const eased = 1 - (1 - progress) ** 4

      setValue(Math.round(from + (target - from) * eased))

      if (progress < 1) {
        frame = requestAnimationFrame(tick)
      } else {
        fromRef.current = target
        setRunning(false)
      }
    }

    // effect 안에서 동기 setState 금지 — 렌더가 한 번 더 돌고 린터에 걸림. 첫 프레임에서 시작
    frame = requestAnimationFrame((now) => {
      // 연출을 끈 사용자에게는 결과만
      if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
        fromRef.current = target
        setValue(target)
        return
      }

      setRunning(true)
      tick(now)
    })

    return () => {
      cancelAnimationFrame(frame)
      fromRef.current = target
    }
  }, [target])

  return { value, running }
}
