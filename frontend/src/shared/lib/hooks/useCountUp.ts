import { useEffect, useRef, useState } from 'react'

/**
 * 숫자가 직전 값에서 새 값으로 굴러간다.
 *
 * 처음 그릴 때는 움직이지 않는다. 아무 일도 없었는데 움직이는 셈이고, 화면을 열 때마다
 * 0부터 세면 값을 읽기까지 기다려야 한다. 값이 실제로 바뀌었을 때만 움직인다.
 * 그때는 그 움직임 자체가 "얼마나 올랐는지"를 말해 준다.
 *
 * 지속 시간은 변화 폭에 비례시킨다. 10km 와 20,000km 가 같은 시간이면 작은 변화는
 * 굼뜨고 큰 변화는 순식간에 지나간다.
 *
 * running 은 글자 폭 때문에 돌려준다. 매 프레임 값이 바뀌는 동안 글자 폭이 다르면
 * 숫자가 떨려서, 움직이는 동안에만 tabular-nums 를 붙인다.
 */
export function useCountUp(target: number): { value: number; running: boolean } {
  const [value, setValue] = useState(target)
  const [running, setRunning] = useState(false)

  // 직전에 떠 있던 값. 첫 렌더에서는 target 과 같아서 아무 일도 일어나지 않는다.
  const fromRef = useRef(target)

  useEffect(() => {
    const from = fromRef.current
    if (from === target) {
      return
    }

    // 1,000 당 약 0.1초, 0.45~1.4초 사이로 묶는다.
    const distance = Math.abs(target - from)
    const duration = Math.min(1400, Math.max(450, (distance / 1000) * 100 + 400))

    const start = performance.now()
    let frame = 0

    function tick(now: number) {
      const progress = Math.min(1, (now - start) / duration)
      // ease-out quart. CSS 의 ease-apple 과 같은 성격이라 한 몸으로 보인다.
      const eased = 1 - (1 - progress) ** 4

      setValue(Math.round(from + (target - from) * eased))

      if (progress < 1) {
        frame = requestAnimationFrame(tick)
      } else {
        fromRef.current = target
        setRunning(false)
      }
    }

    // effect 안에서 동기적으로 setState 하지 않는다. 렌더가 한 번 더 돌고
    // oxlint react(set-state-in-effect) 에 걸린다. 첫 프레임에서 시작한다.
    frame = requestAnimationFrame((now) => {
      // 연출을 끈 사용자에게는 결과만 준다.
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
