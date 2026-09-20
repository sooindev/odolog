import { act, renderHook } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { useCountUp } from './useCountUp'

/** jsdom 에 matchMedia 가 없음. 연출을 끈 사용자 분기를 타려면 직접 심어야 함 */
function setReducedMotion(reduced: boolean) {
  vi.stubGlobal('matchMedia', (query: string) => ({
    matches: reduced,
    media: query,
    addEventListener: () => {},
    removeEventListener: () => {},
  }))
}

/** 애니메이션이 끝날 때까지 프레임을 흘려보냄 */
async function runToEnd() {
  await act(async () => {
    vi.advanceTimersByTime(2000)
  })
}

describe('useCountUp', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['requestAnimationFrame', 'cancelAnimationFrame', 'performance'] })
    setReducedMotion(false)
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.unstubAllGlobals()
  })

  it('첫 렌더에서는 움직이지 않는다', async () => {
    // 0 에서 굴러오르는 연출은 아무 일도 없었는데 움직이는 것 — 일부러 걷어낸 자리
    const { result } = renderHook(() => useCountUp(50_000))

    expect(result.current.value).toBe(50_000)
    expect(result.current.running).toBe(false)

    await runToEnd()
    expect(result.current.value).toBe(50_000)
    expect(result.current.running).toBe(false)
  })

  it('값이 바뀌면 직전 값에서 새 값으로 굴러간다', async () => {
    const { result, rerender } = renderHook(({ target }) => useCountUp(target), {
      initialProps: { target: 50_000 },
    })

    rerender({ target: 52_000 })

    // 첫 프레임에 시작. 중간값은 두 값 사이에 있어야 함
    await act(async () => {
      vi.advanceTimersByTime(100)
    })
    expect(result.current.running).toBe(true)
    expect(result.current.value).toBeGreaterThan(50_000)
    expect(result.current.value).toBeLessThan(52_000)

    await runToEnd()
    expect(result.current.value).toBe(52_000)
    expect(result.current.running).toBe(false)
  })

  it('굴러간 뒤 또 바뀌면 그 자리에서 이어간다', async () => {
    // fromRef 가 갱신되지 않으면 두 번째 변화가 첫 값에서 다시 시작함
    const { result, rerender } = renderHook(({ target }) => useCountUp(target), {
      initialProps: { target: 50_000 },
    })

    rerender({ target: 52_000 })
    await runToEnd()

    rerender({ target: 52_500 })
    await act(async () => {
      vi.advanceTimersByTime(100)
    })
    expect(result.current.value).toBeGreaterThan(52_000)

    await runToEnd()
    expect(result.current.value).toBe(52_500)
  })

  it('변화 폭이 클수록 오래 걸린다', async () => {
    // 10km 와 20,000km 가 같은 시간이면 작은 변화는 굼뜨고 큰 변화는 순식간에 지나감
    const small = renderHook(({ target }) => useCountUp(target), {
      initialProps: { target: 50_000 },
    })
    const large = renderHook(({ target }) => useCountUp(target), {
      initialProps: { target: 50_000 },
    })

    small.rerender({ target: 50_010 })
    large.rerender({ target: 70_000 })

    await act(async () => {
      vi.advanceTimersByTime(500)
    })

    // 작은 변화는 최소 지속 시간(0.45s) 안에 끝나고, 큰 변화는 아직 가는 중
    expect(small.result.current.running).toBe(false)
    expect(large.result.current.running).toBe(true)

    await runToEnd()
    expect(large.result.current.value).toBe(70_000)
  })

  it('연출을 끈 사용자에게는 결과만 준다', async () => {
    setReducedMotion(true)

    const { result, rerender } = renderHook(({ target }) => useCountUp(target), {
      initialProps: { target: 50_000 },
    })

    rerender({ target: 52_000 })
    await act(async () => {
      vi.advanceTimersByTime(100)
    })

    expect(result.current.value).toBe(52_000)
    expect(result.current.running).toBe(false)
  })
})
