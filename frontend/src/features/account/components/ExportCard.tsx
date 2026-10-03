import { useState } from 'react'

import { Button } from '@/shared/ui/base/button'
import { Card, CardContent } from '@/shared/ui/base/card'
import { ErrorText } from '@/shared/ui/state'
import { useI18n } from '@/shared/i18n/I18nContext'
import { errorMessage } from '@/shared/i18n/errorMessage'
import { todayString } from '@/shared/lib/format'
import { exportAccount } from '@/features/account/api/endpoints'
import { RestoreForm } from '@/features/account/components/RestoreForm'

export function ExportCard() {
  const { t, timeZone } = useI18n()
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  async function handleExport() {
    setError(null)
    setPending(true)

    try {
      const data = await exportAccount()

      // <a href> 대신 fetch 후 파일 생성. 세션·CSRF 헤더 유지
      const url = URL.createObjectURL(
        new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' }),
      )
      const link = document.createElement('a')
      link.href = url
      link.download = `odolog-${todayString(timeZone)}.json`
      link.click()

      // revoke 는 다음 틱에. 즉시 해제 시 다운로드 취소·0바이트 파일
      setTimeout(() => URL.revokeObjectURL(url), 0)
    } catch (caught) {
      setError(errorMessage(caught, t, t.profile.data.exportFailed))
    } finally {
      setPending(false)
    }
  }

  return (
    <Card>
      <CardContent className="flex flex-col gap-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <p className="min-w-0 text-caption text-muted-foreground">{t.profile.data.exportNote}</p>
          <Button variant="secondary" onClick={handleExport} disabled={pending} className="shrink-0">
            {pending ? t.profile.data.exporting : t.profile.data.export}
          </Button>
        </div>
        {error !== null && <ErrorText message={error} />}

        <div className="border-t border-border pt-4">
          <RestoreForm />
        </div>
      </CardContent>
    </Card>
  )
}
