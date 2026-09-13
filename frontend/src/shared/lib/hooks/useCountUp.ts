import { useEffect, useRef, useState } from 'react'

/**
 * 숫자가 **직전 값에서 새 값으로** 굴러 올라가는 연출.
 *
 * 처음 화면에 그려질 때는 움직이지 않는다. 0 에서 굴러 오르는 연출은 대시보드 템플릿의
 * 상투구이고, 무엇보다 **아무 일도 일어나지 않았는데 움직이는 것**이다. 차량 상세를 열
 * 때마다 주행거리가 0부터 세어 올라가면 두 번째부터는 값을 읽기까지 기다려야 한다.
 *
 * 값이 실제로 바뀌었을 때만 움직인다. 주행거리를 50,000 → 52,000 으로 갱신하면 그 구간만
 * 굴러가고, 그 움직임이 곧 **"2,000km 올랐다"** 는 뜻을 나른다. 연출이 정보가 되는 경우다.
 *
 * 그래서 지속 시간도 값의 변화 폭에 따라 달라진다. 10km 올린 것과 20,000km 올린 것이
 * 같은 시간 동안 굴러가면, 작은 변화는 굼뜨게 느껴지고 큰 변화는 순식간에 지나간다.
 *
 * `running` 을 함께 돌려주는 이유는 글자 폭이다. 디자인 시스템 7번은 큰 숫자에
 * `tabular-nums` 를 쓰지 말라고 한다(맞출 상대가 없는데 폭만 벌어져서). 그건 멈춰 있는
 * 숫자 이야기이고, 매 프레임 값이 바뀌는 동안 글자마다 폭이 다르면 숫자가 덜덜 떨린다.
 * **움직이는 동안에만** 폭을 고정한다.
 */
export function useCountUp(target: number): { value: number; running: boolean } {
  const [value, setValue] = useState(target)
  const [running, setRunning] = useState(false)

  // 직전에 화면에 떠 있던 값. 첫 렌더에서는 target 과 같으므로 아무 일도 일어나지 않는다.
  const fromRef = useRef(target)

  useEffect(() => {
    const from = fromRef.current
    if (from === target) {
      return
    }

    // 변화 폭에 시간을 맞춘다. 1,000km 당 약 0.1초, 0.45~1.4초 사이로 묶는다.
    const distance = Math.abs(target - from)
    const duration = Math.min(1400, Math.max(450, (distance / 1000) * 100 + 400))

    const start = performance.now()
    let frame = 0

    function tick(now: number) {
      const progress = Math.min(1, (now - start) / duration)
      // ease-out quart — CSS 쪽 ease-apple 과 같은 성격이라 한 몸으로 보인다.
      const eased = 1 - (1 - progress) ** 4

      setValue(Math.round(from + (target - from) * eased))

      if (progress < 1) {
        frame = requestAnimationFrame(tick)
      } else {
        fromRef.current = target
        setRunning(false)
      }
    }

    // setState 를 effect 안에서 동기적으로 부르지 않는다 — 렌더가 한 번 더 돌고
    // oxlint 의 react(set-state-in-effect) 에도 걸린다. 첫 프레임에서 시작한다.
    frame = requestAnimationFrame((now) => {
      // 연출을 끄기로 한 사용자에게는 결과만 준다. 이 판정도 첫 프레임 안에서 한다 —
      // effect 본문에서 바로 setState 하면 렌더가 한 번 더 돈다(oxlint set-state-in-effect).
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
