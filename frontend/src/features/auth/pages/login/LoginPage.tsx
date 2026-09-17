import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router'

import { useAuth } from '@/features/auth/context/definition/AuthContext'
import { Button } from '@/shared/ui/base/button'
import { Card, CardContent } from '@/shared/ui/base/card'
import { Field } from '@/shared/ui/form/field'
import { Input } from '@/shared/ui/base/input'
import { FormActions, Page } from '@/shared/ui/layout/page'
import { ErrorText } from '@/shared/ui/feedback/state'
import { ApiError } from '@/shared/api/client/client'

export function LoginPage() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  // ProtectedRoute 가 기억해 둔 목적지. 없으면 차량 목록
  const from = (location.state as { from?: string } | null)?.from ?? '/vehicles'

  if (user !== null) {
    return <Navigate to={from} replace />
  }

  async function handleSubmit(event: FormEvent) {
    // 폼 기본 동작(전체 새로고침) 차단. 안 막으면 React 상태가 날아감
    event.preventDefault()
    setError(null)
    setPending(true)

    try {
      await login({ email, password })
      navigate(from, { replace: true })
    } catch (caught) {
      // 백엔드가 401 사유를 통일해 내려줌 (user enumeration 방지)
      setError(caught instanceof ApiError ? caught.message : '로그인에 실패했습니다.')
    } finally {
      setPending(false)
    }
  }

  return (
    <Page title="로그인" description="기록해 둔 차량을 이어서 관리합니다.">
      {/* 폼 + 안내 문구가 한 덩어리. 가로 폭은 AuthLayout 담당 */}
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

              {error !== null && <ErrorText message={error} />}

              <FormActions>
                <Button type="submit" disabled={pending}>
                  {pending ? '로그인 중…' : '로그인'}
                </Button>
              </FormActions>
            </form>
          </CardContent>
        </Card>

        <p className="text-center text-sm text-muted-foreground">
          계정이 없으신가요?{' '}
          <Link
            to="/signup"
            className="text-strong transition-opacity duration-200 ease-apple hover:opacity-70"
          >
            회원가입
          </Link>
        </p>
      </div>
    </Page>
  )
}
