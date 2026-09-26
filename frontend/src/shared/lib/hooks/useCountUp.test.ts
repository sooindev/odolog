import { act, renderHook } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { useCountUp } from './useCountUp'

/** jsdom 에 없는 matchMedia 대체 */
function setReducedMotion(reduced: boolean) {
  vi.stubGlobal('matchMedia', (query: string) => ({
    matches: reduced,
    media: query,
    addEventListener: () => {},
    removeEventListener: () => {},
  }))
}

/** 애니메이션 종료까지 프레임 진행 */
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
    // 첫 렌더 정지
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

    // 중간값은 두 값 사이
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
    // 두 번째 변화는 첫 목표에서 출발
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
    // 변화 폭에 비례한 지속 시간
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

    // 작은 변화는 최소 0.45s 안에 종료, 큰 변화는 진행 중
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

  it('굴러가는 도중 지금 보이는 값으로 목표가 바뀌면 연출이 끝난 상태로 돌아간다', async () => {
    // 멈춘 숫자에 tabular-nums 가 남지 않음
    const { result, rerender } = renderHook(({ target }) => useCountUp(target), {
      initialProps: { target: 50_000 },
    })

    rerender({ target: 60_000 })
    await act(async () => {
      vi.advanceTimersByTime(200)
    })
    const shown = result.current.value
    expect(result.current.running).toBe(true)

    rerender({ target: shown })
    await runToEnd()

    expect(result.current.value).toBe(shown)
    expect(result.current.running).toBe(false)
  })
})
