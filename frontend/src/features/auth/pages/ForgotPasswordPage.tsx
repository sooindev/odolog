import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router'

import { requestPasswordReset } from '@/features/auth/api/endpoints'
import { useI18n } from '@/shared/i18n/I18nContext'
import { errorMessage } from '@/shared/i18n/errorMessage'
import { Button } from '@/shared/ui/base/button'
import { Card, CardContent } from '@/shared/ui/base/card'
import { Input } from '@/shared/ui/base/input'
import { ErrorText, NoticeText } from '@/shared/ui/state'
import { Field } from '@/shared/ui/form/field'
import { FormActions, Page } from '@/shared/ui/layout/page'

export function ForgotPasswordPage() {
  const { t } = useI18n()
  const [email, setEmail] = useState('')
  const [sent, setSent] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setPending(true)

    try {
      await requestPasswordReset({ email })
      setSent(true)
    } catch (caught) {
      setError(errorMessage(caught, t, t.forgotPassword.failed))
    } finally {
      setPending(false)
    }
  }

  return (
    // 로그인 전이라 eyebrow 없음
    <Page title={t.forgotPassword.title} description={t.forgotPassword.description}>
      <div className="flex flex-col gap-6">
        <Card>
          <CardContent>
            {sent ? (
              <div className="flex flex-col gap-4">
                {/* 가입 여부 비공개 */}
                <NoticeText message={t.forgotPassword.sent} />
                <p className="text-caption leading-relaxed text-muted-foreground">
                  {t.forgotPassword.sentDetail}
                </p>
              </div>
            ) : (
              <form className="flex flex-col gap-5" onSubmit={handleSubmit}>
                <Field label={t.common.email} htmlFor="email" hint={t.forgotPassword.hint}>
                  <Input
                    id="email"
                    type="email"
                    required
                    autoFocus
                    maxLength={100}
                    autoComplete="email"
                    placeholder={t.common.emailPlaceholder}
                    value={email}
                    onChange={(event) => setEmail(event.target.value)}
                  />
                </Field>

                {error !== null && <ErrorText message={error} />}

                <FormActions>
                  <Button type="submit" disabled={pending}>
                    {pending ? t.forgotPassword.submitting : t.forgotPassword.submit}
                  </Button>
                </FormActions>
              </form>
            )}
          </CardContent>
        </Card>

        <p className="text-center text-caption text-muted-foreground">
          <Link
            to="/login"
            className="text-strong transition-opacity duration-200 ease-apple hover:opacity-70"
          >
            {t.forgotPassword.backToLogin}
          </Link>
        </p>
      </div>
    </Page>
  )
}
