import { useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import type { ChangeEvent } from 'react'

import { ErrorText, NoticeText } from '@/shared/ui/state'
import { useI18n } from '@/shared/i18n/I18nContext'
import { errorMessage } from '@/shared/i18n/errorMessage'
import type { Messages } from '@/shared/i18n/messages/ko'
import { restoreAccount } from '@/features/account/api/endpoints'
import { MAX_BODY_BYTES } from '@/shared/lib/limits'
import { queryKeys } from '@/shared/api/queryKeys'
import type { AccountExport, AccountRestoreResult } from '@/features/account/api/types'

/**
 * 내보낸 파일 복원. 내보내기 바로 아래 배치
 * 브라우저에서 읽어 JSON 전송. multipart 미사용
 */
export function RestoreForm() {
  const { t } = useI18n()
  const queryClient = useQueryClient()
  const [result, setResult] = useState<AccountRestoreResult | null>(null)
  // 문구가 아니라 문구를 고르는 함수. 언어를 바꾸면 남은 안내도 새 언어로
  const [error, setError] = useState<((messages: Messages) => string) | null>(null)
  const [pending, setPending] = useState(false)

  async function handleFile(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    // 같은 파일 재선택 허용
    event.target.value = ''
    if (file === undefined) {
      return
    }

    setResult(null)
    setError(null)
    // 서버는 상한을 넘으면 연결을 끊을 수 있어 "연결 실패" 로 보임. 보내기 전에 막음
    if (file.size > MAX_BODY_BYTES) {
      setError(() => (messages: Messages) => messages.errors.codes.PAYLOAD_TOO_LARGE)
      return
    }

    setPending(true)

    try {
      const parsed = JSON.parse(await file.text()) as { vehicles?: AccountExport['vehicles'] } | null
      // null·숫자 같은 객체 아닌 JSON 도 형식이 다른 파일
      if (typeof parsed !== 'object' || parsed === null || !Array.isArray(parsed.vehicles)) {
        // 형식이 다른 파일은 전송 전 안내
        throw new SyntaxError('vehicles missing')
      }

      setResult(await restoreAccount(parsed.vehicles))
      // 기록이 더해진 차량을 옛 캐시로 보이지 않게. 다시 열면 처음부터 읽음
      queryClient.removeQueries({ queryKey: queryKeys.allVehicles() })
    } catch (caught) {
      setError(() => (messages: Messages) =>
        caught instanceof SyntaxError
          ? messages.profile.data.notOurFile
          : errorMessage(caught, messages, messages.profile.data.importFailed),
      )
    } finally {
      setPending(false)
    }
  }

  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <p className="min-w-0 text-caption text-muted-foreground">{t.profile.data.importNote}</p>
        {/* label 로 감싼 버튼형 파일 입력. 기본 모양은 테마 미반영 */}
        {/* 입력이 span 앞에 있어야 peer 로 포커스를 그릴 수 있음. 입력 자체는 sr-only 라 안 보임 */}
        <label className="shrink-0">
          <input
            type="file"
            accept="application/json,.json"
            className="peer sr-only"
            disabled={pending}
            onChange={handleFile}
          />
          <span
            className={
              'inline-flex h-9 cursor-pointer items-center border border-border bg-fill px-4 ' +
              'text-caption text-strong transition-colors duration-200 ease-apple hover:bg-card-hover ' +
              'peer-focus-visible:ring-2 peer-focus-visible:ring-ring peer-focus-visible:ring-offset-2 ' +
              'peer-focus-visible:ring-offset-background'
            }
          >
            {pending ? t.profile.data.importing : t.profile.data.import}
          </span>
        </label>
      </div>

      {/* 건너뛴 수까지 안내. 새로 넣은 것이 없으면 "0건을 넣었습니다" 대신 없다고 말함 */}
      {result !== null && (
        <NoticeText
          message={
            (result.addedVehicles === 0 &&
            result.addedMaintenanceRecords + result.addedFuelRecords === 0 &&
            result.addedServiceIntervals === 0
              ? t.profile.data.nothingNew
              : t.profile.data.imported(
                  result.addedVehicles,
                  result.addedMaintenanceRecords + result.addedFuelRecords,
                ) +
                (result.mergedVehicles > 0 ? t.profile.data.merged(result.mergedVehicles) : '') +
                (result.addedServiceIntervals > 0 ? t.profile.data.intervals(result.addedServiceIntervals) : '')) +
            (result.skippedRecords > 0 ? t.profile.data.skipped(result.skippedRecords) : '')
          }
        />
      )}

      {error !== null && <ErrorText message={error(t)} />}
    </div>
  )
}
