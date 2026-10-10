import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router'

import { useAuth } from '@/features/auth/context/AuthContext'
import { Button } from '@/shared/ui/base/button'
import { Card, CardContent } from '@/shared/ui/base/card'
import { Field } from '@/shared/ui/form/field'
import { Input } from '@/shared/ui/base/input'
import { FormActions, Page } from '@/shared/ui/layout/page'
import { ErrorText, NoticeText } from '@/shared/ui/state'
import { useI18n } from '@/shared/i18n/I18nContext'
import { errorMessage } from '@/shared/i18n/errorMessage'

export function LoginPage() {
  const { user, login } = useAuth()
  const { t } = useI18n()
  const navigate = useNavigate()
  const location = useLocation()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  // 이전 화면이 남긴 목적지(from)·안내(notice). 이번 이동에만 존재
  // notice 는 문장이 아니라 키. 언어는 이 화면이 정함
  const state = location.state as { from?: string; notice?: keyof typeof t.login.notices } | null
  const from = state?.from ?? '/vehicles'
  // 첫 렌더의 안내만 붙잡아 둠. history.state 는 새로고침에도 남기 때문
  const [noticeKey] = useState(state?.notice)
  const notice = noticeKey === undefined ? null : t.login.notices[noticeKey]

  useEffect(() => {
    // 안내를 뺀 state 로 바꿔치기. from 은 남김
    if (state?.notice !== undefined) {
      navigate(location.pathname, { replace: true, state: state.from === undefined ? null : { from: state.from } })
    }
  }, [state, location.pathname, navigate])

  if (user !== null) {
    return <Navigate to={from} replace />
  }

  async function handleSubmit(event: FormEvent) {
    // 기본 제출(새로고침) 차단
    event.preventDefault()
    setError(null)
    setPending(true)

    try {
      await login({ email, password })
      navigate(from, { replace: true })
    } catch (caught) {
      // 401 사유는 서버가 통일
      setError(errorMessage(caught, t, t.login.failed))
    } finally {
      setPending(false)
    }
  }

  return (
    <Page title={t.login.title} description={t.login.description}>
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
                  autoComplete="email"
                  placeholder={t.common.emailPlaceholder}
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                />
              </Field>

              <Field label={t.common.password} htmlFor="password">
                <Input
                  id="password"
                  type="password"
                  required
                  autoComplete="current-password"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                />
              </Field>

              {/* 실패가 있으면 실패만 표시 */}
              {error !== null ? (
                <ErrorText message={error} />
              ) : (
                notice !== null && <NoticeText message={notice} />
              )}

              <FormActions>
                <Button type="submit" disabled={pending}>
                  {pending ? t.login.submitting : t.login.submit}
                </Button>
              </FormActions>
            </form>
          </CardContent>
        </Card>

        <p className="text-center text-caption text-muted-foreground">
          {t.login.noAccount}{' '}
          <Link
            to="/signup"
            className="text-strong transition-opacity duration-200 ease-apple hover:opacity-70"
          >
            {t.login.signUp}
          </Link>
        </p>

        <p className="text-center text-caption text-muted-foreground">
          <Link
            to="/forgot-password"
            className="transition-opacity duration-200 ease-apple hover:opacity-70"
          >
            {t.login.forgot}
          </Link>
        </p>
      </div>
    </Page>
  )
}
