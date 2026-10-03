import { useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate } from 'react-router'

import { useAuth } from '@/features/auth/context/AuthContext'
import { Button } from '@/shared/ui/base/button'
import { Card, CardContent } from '@/shared/ui/base/card'
import { Input } from '@/shared/ui/base/input'
import { Field } from '@/shared/ui/form/field'
import { FormActions } from '@/shared/ui/layout/page'
import { ErrorText } from '@/shared/ui/state'
import { useI18n } from '@/shared/i18n/I18nContext'
import { errorMessage } from '@/shared/i18n/errorMessage'

/** 탈퇴 요청은 세션 종료까지 함께라 AuthContext 의 withdraw 사용 */
export function WithdrawCard() {
  const { withdraw } = useAuth()
  const { t } = useI18n()
  const navigate = useNavigate()

  const [open, setOpen] = useState(false)
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setPending(true)

    try {
      // replace: 뒤로가기로 복귀 방지. 이동은 상태 지우기와 함께(AuthProvider)
      await withdraw({ password }, () => navigate('/', { replace: true }))
    } catch (caught) {
      // 401 = 비밀번호 오류. 전역 401 처리 제외 경로
      setError(errorMessage(caught, t, t.profile.withdraw.failed))
      // 성공 시 화면 이탈, 실패 시에만 복구
      setPending(false)
    }
  }

  return (
    <Card>
      <CardContent>
        {open ? (
          <div className="form-open">
            <div>
              <form className="flex flex-col gap-5" onSubmit={handleSubmit}>
                {/* 본인 확인용 비밀번호 */}
                <Field
                  label={t.common.password}
                  htmlFor="withdraw-password"
                  hint={t.profile.withdraw.passwordHint}
                >
                  <Input
                    id="withdraw-password"
                    type="password"
                    required
                    autoComplete="current-password"
                    value={password}
                    onChange={(event) => setPassword(event.target.value)}
                  />
                </Field>

                {error !== null && <ErrorText message={error} />}

                <FormActions>
                  <Button type="submit" variant="destructive" disabled={pending}>
                    {pending ? t.profile.withdraw.submitting : t.profile.withdraw.submit}
                  </Button>
                  <Button
                    type="button"
                    variant="ghost"
                    onClick={() => {
                      setOpen(false)
                      setPassword('')
                      setError(null)
                    }}
                  >
                    {t.common.cancel}
                  </Button>
                </FormActions>
              </form>
            </div>
          </div>
        ) : (
          <div className="flex flex-wrap items-center justify-between gap-3">
            {/* min-w-0: 버튼 밀림 방지 */}
            <p className="min-w-0 text-caption text-muted-foreground">{t.profile.withdraw.note}</p>
            {/* 채우지 않은 빨간 버튼 */}
            <Button
              variant="destructive"
              size="sm"
              className="shrink-0"
              onClick={() => setOpen(true)}
            >
              {t.profile.withdraw.open}
            </Button>
          </div>
        )}
      </CardContent>
    </Card>
  )
}
