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
  // 지금 화면에 찍혀 있는 값. 연출 도중에 target 이 또 바뀌면 여기서 이어간다
  const shownRef = useRef(target)

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

      shownRef.current = Math.round(from + (target - from) * eased)
      setValue(shownRef.current)

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
        shownRef.current = target
        setValue(target)
        return
      }

      setRunning(true)
      tick(now)
    })

    return () => {
      cancelAnimationFrame(frame)
      /*
       * 끝까지 갔으면 shownRef 가 곧 target 이라 결과가 같고,
       * 도중에 끊겼으면 **화면에 보이던 그 값**에서 다음 연출이 출발한다.
       * 전에는 옛 target 을 넣어서, 굴러가는 중에 값이 또 바뀌면 숫자가 한 번 튀었다
       */
      fromRef.current = shownRef.current
    }
  }, [target])

  return { value, running }
}
