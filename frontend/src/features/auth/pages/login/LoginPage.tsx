import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router'

import { useAuth } from '@/features/auth/context/definition/AuthContext'
import { Button } from '@/shared/ui/base/button'
import { Card, CardContent } from '@/shared/ui/base/card'
import { Field } from '@/shared/ui/form/field'
import { Input } from '@/shared/ui/base/input'
import { FormActions, Page } from '@/shared/ui/layout/page'
import { ErrorText, NoticeText } from '@/shared/ui/feedback/state'
import { ApiError } from '@/shared/api/client/client'

export function LoginPage() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  // 이전 화면이 남긴 목적지(from)·안내(notice). 이번 이동에만 존재
  const state = location.state as { from?: string; notice?: string } | null
  const from = state?.from ?? '/vehicles'
  const notice = state?.notice ?? null

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
      setError(caught instanceof ApiError ? caught.message : '로그인에 실패했습니다.')
    } finally {
      setPending(false)
    }
  }

  return (
    <Page title="로그인" description="기록해 둔 차량을 이어서 관리합니다.">
      {/* 폼 + 안내 문구 한 덩어리. 폭은 AuthLayout 담당 */}
      <div className="flex flex-col gap-6">
        <Card>
          <CardContent>
            <form className="flex flex-col gap-5" onSubmit={handleSubmit}>
              <Field label="이메일" htmlFor="email">
                <Input
                  id="email"
                  type="email"
                  required
                  autoComplete="email"
                  placeholder="you@example.com"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                />
              </Field>

              <Field label="비밀번호" htmlFor="password">
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
                  {pending ? '로그인 중…' : '로그인'}
                </Button>
              </FormActions>
            </form>
          </CardContent>
        </Card>

        <p className="text-center text-caption text-muted-foreground">
          계정이 없으신가요?{' '}
          <Link
            to="/signup"
            className="text-strong transition-opacity duration-200 ease-apple hover:opacity-70"
          >
            회원가입
          </Link>
        </p>

        <p className="text-center text-caption text-muted-foreground">
          <Link
            to="/forgot-password"
            className="transition-opacity duration-200 ease-apple hover:opacity-70"
          >
            비밀번호를 잊으셨나요?
          </Link>
        </p>
      </div>
    </Page>
  )
}
