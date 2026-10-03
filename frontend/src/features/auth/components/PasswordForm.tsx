import { useState } from 'react'
import type { FormEvent } from 'react'

import { changePassword } from '@/features/auth/api/endpoints'
import { Button } from '@/shared/ui/base/button'
import { Card, CardContent } from '@/shared/ui/base/card'
import { Input } from '@/shared/ui/base/input'
import { Field } from '@/shared/ui/form/field'
import { FormActions } from '@/shared/ui/layout/page'
import { ErrorText, NoticeText } from '@/shared/ui/state'
import { useI18n } from '@/shared/i18n/I18nContext'
import { errorMessage } from '@/shared/i18n/errorMessage'
import { passwordHint } from '@/shared/lib/limits'

export function PasswordForm() {
  const { t } = useI18n()
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setMessage(null)
    setError(null)

    // 확인란은 전송하지 않음
    if (newPassword !== confirmPassword) {
      setError(t.password.mismatch)
      return
    }

    setPending(true)

    try {
      await changePassword({ currentPassword, newPassword })
      setMessage(t.profile.password.changed)
      // 성공 시 입력칸 비움
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
    } catch (caught) {
      // 401 = 현재 비밀번호 오류
      setError(errorMessage(caught, t, t.profile.password.failed))
    } finally {
      setPending(false)
    }
  }

  return (
    <Card>
      <CardContent>
        <form className="flex flex-col gap-5" onSubmit={handleSubmit}>
          {/* autoComplete 로 비밀번호 관리자의 현재·새 비밀번호 구분 */}
          <Field label={t.password.current} htmlFor="current-password">
            <Input
              id="current-password"
              type="password"
              required
              autoComplete="current-password"
              value={currentPassword}
              onChange={(event) => setCurrentPassword(event.target.value)}
            />
          </Field>

          <div className="grid gap-5 sm:grid-cols-2">
            <Field label={t.password.newPassword} htmlFor="new-password" hint={passwordHint(newPassword, t)}>
              <Input
                id="new-password"
                type="password"
                required
                minLength={8}
                // 글자 수 상한은 대략적인 천장. 실제 판정은 hint 와 서버 @MaxBytes
                maxLength={72}
                autoComplete="new-password"
                value={newPassword}
                onChange={(event) => setNewPassword(event.target.value)}
              />
            </Field>

            <Field label={t.password.confirm} htmlFor="confirm-password">
              <Input
                id="confirm-password"
                type="password"
                required
                autoComplete="new-password"
                value={confirmPassword}
                onChange={(event) => setConfirmPassword(event.target.value)}
              />
            </Field>
          </div>

          {message !== null && <NoticeText message={message} />}
          {error !== null && <ErrorText message={error} />}

          <FormActions>
            <Button type="submit" disabled={pending}>
              {pending ? t.profile.password.submitting : t.profile.password.submit}
            </Button>
          </FormActions>
        </form>
      </CardContent>
    </Card>
  )
}
