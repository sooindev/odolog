import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router'

import { useAuth } from '@/auth/AuthContext'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { ApiError } from '@/lib/api'

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
    <Card className="mx-auto max-w-sm">
      <CardHeader>
        <CardTitle>로그인</CardTitle>
      </CardHeader>
      <CardContent>
        <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="email">이메일</Label>
            <Input
              id="email"
              type="email"
              required
              autoComplete="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="password">비밀번호</Label>
            <Input
              id="password"
              type="password"
              required
              autoComplete="current-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
            />
          </div>

          {error !== null && <p className="text-destructive text-sm">{error}</p>}

          <Button type="submit" disabled={pending}>
            {pending ? '로그인 중…' : '로그인'}
          </Button>

          <p className="text-muted-foreground text-center text-sm">
            계정이 없으신가요?{' '}
            <Link to="/signup" className="underline">
              회원가입
            </Link>
          </p>
        </form>
      </CardContent>
    </Card>
  )
}
