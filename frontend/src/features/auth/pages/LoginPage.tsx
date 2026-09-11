import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router'

import { useAuth } from '@/features/auth/context/AuthContext'
import { Button } from '@/shared/ui/button'
import { Field } from '@/shared/ui/field'
import { Input } from '@/shared/ui/input'
import { PageHeader } from '@/shared/ui/page-header'
import { ErrorText } from '@/shared/ui/state'
import { ApiError } from '@/shared/api/client'

export function LoginPage() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  // ProtectedRoute가 기억해 둔 "원래 가려던 곳". 없으면 차량 목록으로.
  const from = (location.state as { from?: string } | null)?.from ?? '/vehicles'

  if (user !== null) {
    return <Navigate to={from} replace />
  }

  async function handleSubmit(event: FormEvent) {
    // 폼 기본 동작(페이지 전체 새로고침)을 막는다. 안 막으면 React 상태가 다 날아간다.
    event.preventDefault()
    setError(null)
    setPending(true)

    try {
      await login({ email, password })
      navigate(from, { replace: true })
    } catch (caught) {
      // 백엔드가 401에 "이메일 또는 비밀번호가 올바르지 않습니다"로 사유를 통일해 내려준다.
      setError(caught instanceof ApiError ? caught.message : '로그인에 실패했습니다.')
    } finally {
      setPending(false)
    }
  }

  return (
    // 폼을 카드에 넣지 않는다. 바탕 위에 입력창만 떠 있는 편이 훨씬 조용하고,
    // 카드 테두리가 없어지면 화면의 선이 입력창 개수만큼으로 줄어든다.
    // 가로 폭(22rem)은 AuthLayout이 정한다 — 두 화면이 같은 폭이어야 하기 때문.
    <div className="flex flex-col gap-10">
      <PageHeader eyebrow="Odolog" title="로그인" description="기록해 둔 차량을 이어서 관리합니다." />

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

        <Button type="submit" size="lg" className="mt-1 w-full" disabled={pending}>
          {pending ? '로그인 중…' : '로그인'}
        </Button>
      </form>

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
  )
}
