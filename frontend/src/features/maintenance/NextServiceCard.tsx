import { useEffect, useState } from 'react'

import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
import { formatKm } from '@/shared/lib/format'
import { fetchNextService } from '@/features/maintenance/api'
import { SERVICE_TYPES, SERVICE_TYPE_LABELS } from '@/shared/api/types'
import type { NextServiceResponse } from '@/shared/api/types'

/**
 * 다음 정비 시점을 종류별로 보여준다.
 * 백엔드 API가 종류 하나씩만 계산하므로 요청이 종류 수만큼 나간다.
 * 실제로 느려지면 "전체 종류 한 번에" API 추가를 검토한다 (CLAUDE.md 백로그).
 */
export function NextServiceCard({ vehicleId, reloadKey }: { vehicleId: number; reloadKey: number }) {
  const [results, setResults] = useState<NextServiceResponse[] | null>(null)

  useEffect(() => {
    let cancelled = false

    async function load() {
      try {
        // Promise.all: 5개 요청을 순서대로 기다리지 않고 동시에 보낸다.
        const all = await Promise.all(
          SERVICE_TYPES.map((type) => fetchNextService(vehicleId, type)),
        )
        if (!cancelled) setResults(all)
      } catch {
        if (!cancelled) setResults(null)
      }
    }

    load()

    return () => {
      cancelled = true
    }
  }, [vehicleId, reloadKey])

  if (results === null) {
    return null
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">다음 정비 시점</CardTitle>
      </CardHeader>
      <CardContent>
        <ul className="flex flex-col gap-2 text-sm">
          {results.map((result) => (
            <li key={result.type} className="flex items-baseline justify-between gap-4">
              <span className="font-medium">{SERVICE_TYPE_LABELS[result.type]}</span>
              <span className="text-muted-foreground text-right">
                {describe(result)}
              </span>
            </li>
          ))}
        </ul>
      </CardContent>
    </Card>
  )
}

/** 이력 없음 / 권장 주기 없음(OTHER) / 정상 계산됨 세 경우를 문장으로 만든다. */
function describe(result: NextServiceResponse) {
  if (result.lastServiceDate === null) {
    return '이력 없음'
  }

  const parts: string[] = []
  if (result.nextServiceOdometer !== null) {
    parts.push(formatKm(result.nextServiceOdometer))
  }
  if (result.nextServiceDate !== null) {
    parts.push(result.nextServiceDate)
  }

  if (parts.length === 0) {
    return `${result.lastServiceDate} 정비 · 권장 주기 없음`
  }

  return parts.join(' 또는 ')
}
