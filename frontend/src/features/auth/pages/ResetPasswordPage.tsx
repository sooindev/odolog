import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router'

import { confirmPasswordReset } from '@/features/auth/api/endpoints'
import { useAuth } from '@/features/auth/context/AuthContext'
import { useI18n } from '@/shared/i18n/I18nContext'
import { errorMessage } from '@/shared/i18n/errorMessage'
import { Button } from '@/shared/ui/base/button'
import { Card, CardContent } from '@/shared/ui/base/card'
import { Input } from '@/shared/ui/base/input'
import { ErrorText } from '@/shared/ui/state'
import { Field } from '@/shared/ui/form/field'
import { FormActions, Page } from '@/shared/ui/layout/page'
import { passwordHint } from '@/shared/lib/limits'

export function ResetPasswordPage() {
  const { t } = useI18n()
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const { replaceUser } = useAuth()
  const token = params.get('token')

  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)

    // 확인란은 전송하지 않음
    if (newPassword !== confirmPassword) {
      setError(t.password.mismatch)
      return
    }

    setPending(true)

    try {
      await confirmPasswordReset({ token: token ?? '', newPassword })
      // 서버가 모든 세션을 끊음. 화면도 로그아웃 상태로 맞춰야 로그인 화면이 다른 곳으로 보내지 않음
      replaceUser(null)
      // 자동 로그인 없이 로그인 화면으로. 안내는 state 로 전달(새로고침 시 사라짐)
      navigate('/login', { replace: true, state: { notice: 'passwordReset' } })
    } catch (caught) {
      setError(errorMessage(caught, t, t.resetPassword.failed))
    } finally {
      setPending(false)
    }
  }

  // 토큰 없이 직접 들어온 경우
  if (token === null || token === '') {
    return (
      <Page title={t.resetPassword.title} description={t.resetPassword.noTokenDescription}>
        <Card>
          <CardContent className="flex flex-col gap-4">
            <ErrorText message={t.resetPassword.invalidLink} />
            <p className="text-caption leading-relaxed text-muted-foreground">
              {t.resetPassword.invalidLinkDetail}
            </p>
            <FormActions>
              <Button render={<Link to="/forgot-password" />}>{t.resetPassword.requestAgain}</Button>
            </FormActions>
          </CardContent>
        </Card>
      </Page>
    )
  }

  return (
    <Page title={t.resetPassword.title} description={t.resetPassword.description}>
      <Card>
        <CardContent>
          <form className="flex flex-col gap-5" onSubmit={handleSubmit}>
          <Field label={t.password.newPassword} htmlFor="new-password" hint={passwordHint(newPassword, t)}>
            <Input
              id="new-password"
              type="password"
              required
              autoFocus
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

          {error !== null && <ErrorText message={error} />}

          <FormActions>
            <Button type="submit" disabled={pending}>
              {pending ? t.resetPassword.submitting : t.resetPassword.submit}
            </Button>
          </FormActions>
          </form>
        </CardContent>
      </Card>
    </Page>
  )
}
