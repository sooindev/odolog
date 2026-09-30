import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'

import { useAuth } from '@/features/auth/context/definition/AuthContext'
import { Button } from '@/shared/ui/base/button'
import { Card, CardContent } from '@/shared/ui/base/card'
import { Field } from '@/shared/ui/form/field'
import { Input } from '@/shared/ui/base/input'
import { FormActions, Page } from '@/shared/ui/layout/page'
import { ErrorText } from '@/shared/ui/feedback/state'
import { useI18n } from '@/shared/i18n/context/I18nContext'
import { errorMessage } from '@/shared/i18n/errors/errorMessage'
import { passwordHint } from '@/shared/lib/limits/limits'
import { signUp } from '@/features/auth/api/endpoints/endpoints'

export function SignUpPage() {
  const { login } = useAuth()
  // 로그인 전 화면이 쓰던 설정 그대로. 추정 못 한 값의 대비책까지 같아야 가입 뒤 단위·통화가 안 바뀜
  const { t, language, timeZone, currency, unitSystem } = useI18n()
  const navigate = useNavigate()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [nickname, setNickname] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setPending(true)

    try {
      // 설정은 입력칸 없이 지금 화면의 값
      await signUp({ email, password, nickname, language, timeZone, currency, unitSystem })
    } catch (caught) {
      // 409 = 이메일 중복, 400 = 검증 실패
      setError(errorMessage(caught, t, t.signUp.failed))
      setPending(false)
      return
    }

    // 가입은 세션을 만들지 않아 로그인까지 이어서. 로그인 실패는 가입 실패와 별도 처리
    try {
      await login({ email, password })
      navigate('/vehicles', { replace: true })
    } catch {
      navigate('/login', { replace: true, state: { notice: 'signedUp' } })
    }
  }

  return (
    <Page title={t.signUp.title} description={t.signUp.description}>
      {/* 폼 + 안내 문구 한 덩어리. 폭은 AuthLayout 담당 */}
      <div className="flex flex-col gap-6">
        <Card>
          <CardContent>
            <form className="flex flex-col gap-5" onSubmit={handleSubmit}>
              <Field label={t.common.email} htmlFor="email">
                <Input
                  id="email"
                  type="email"
                  required
                  maxLength={100}
                  autoComplete="email"
                  placeholder={t.common.emailPlaceholder}
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                />
              </Field>

              {/* 규칙 안내를 미리 */}
              <Field label={t.common.password} htmlFor="password" hint={passwordHint(password, t)}>
                <Input
                  id="password"
                  type="password"
                  required
                  minLength={8}
                  // 글자 수 상한은 대략적인 천장. 실제 판정은 hint 와 서버 @MaxBytes
                  maxLength={72}
                  autoComplete="new-password"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                />
              </Field>

              <Field label={t.common.nickname} htmlFor="nickname">
                <Input
                  id="nickname"
                  required
                  maxLength={30}
                  autoComplete="nickname"
                  value={nickname}
                  onChange={(event) => setNickname(event.target.value)}
                />
              </Field>

              {error !== null && <ErrorText message={error} />}

              <FormActions>
                <Button type="submit" disabled={pending}>
                  {pending ? t.signUp.submitting : t.signUp.submit}
                </Button>
              </FormActions>

              {/* 가입 전에 읽을 수 있게 버튼 바로 아래 */}
              <p className="text-caption leading-relaxed text-muted-foreground">
                <AgreementLine />
              </p>
            </form>
          </CardContent>
        </Card>

        <p className="text-center text-caption text-muted-foreground">
          {t.signUp.haveAccount}{' '}
          <Link
            to="/login"
            className="text-strong transition-opacity duration-200 ease-apple hover:opacity-70"
          >
            {t.signUp.login}
          </Link>
        </p>
      </div>
    </Page>
  )
}

/** 문장 속 두 링크. 어순이 언어마다 달라 자리표시자로 쪼갬 */
function AgreementLine() {
  const { t } = useI18n()
  const [before, middle, after] = t.signUp.agreement('{terms}', '{privacy}').split(/\{terms\}|\{privacy\}/)

  const linkClass = 'text-strong underline-offset-4 hover:underline'

  return (
    <>
      {before}
      <Link to="/terms" className={linkClass}>
        {t.footer.terms}
      </Link>
      {middle}
      <Link to="/privacy" className={linkClass}>
        {t.footer.privacy}
      </Link>
      {after}
    </>
  )
}
