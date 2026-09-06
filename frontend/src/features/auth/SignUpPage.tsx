import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'

import { useAuth } from '@/features/auth/AuthContext'
import { Button } from '@/shared/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
import { Input } from '@/shared/ui/input'
import { Label } from '@/shared/ui/label'
import { ErrorText } from '@/shared/ui/state'
import { ApiError, api } from '@/shared/api/client'
import type { SignUpRequest, UserResponse } from '@/shared/api/types'

export function SignUpPage() {
  const { login } = useAuth()
  const navigate = useNavigate()

  const [form, setForm] = useState<SignUpRequest>({
    email: '',
    password: '',
    nickname: '',
    phone: '',
  })
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  // 필드가 4개라 상태를 4개 두는 대신 객체 하나로 묶고, 바뀐 키만 덮어쓴다.
  function change(key: keyof SignUpRequest, value: string) {
    setForm((previous) => ({ ...previous, [key]: value }))
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setPending(true)

    try {
      await api.post<UserResponse>('/api/users', form)
      // 가입만으로는 세션이 만들어지지 않는다. 바로 로그인까지 해 주면 사용자가 두 번 입력하지 않는다.
      await login({ email: form.email, password: form.password })
      navigate('/vehicles', { replace: true })
    } catch (caught) {
      // 409면 이미 가입된 이메일, 400이면 검증 실패. 둘 다 백엔드 메시지를 그대로 보여준다.
      setError(caught instanceof ApiError ? caught.message : '회원가입에 실패했습니다.')
    } finally {
      setPending(false)
    }
  }

  return (
    <Card className="mx-auto max-w-sm">
      <CardHeader>
        <CardTitle>회원가입</CardTitle>
      </CardHeader>
      <CardContent>
        <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="email">이메일</Label>
            <Input
              id="email"
              type="email"
              required
              maxLength={100}
              autoComplete="email"
              value={form.email}
              onChange={(event) => change('email', event.target.value)}
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="password">비밀번호</Label>
            <Input
              id="password"
              type="password"
              required
              minLength={8}
              maxLength={100}
              autoComplete="new-password"
              value={form.password}
              onChange={(event) => change('password', event.target.value)}
            />
            <p className="text-muted-foreground text-xs">8자 이상</p>
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="nickname">닉네임</Label>
            <Input
              id="nickname"
              required
              maxLength={30}
              value={form.nickname}
              onChange={(event) => change('nickname', event.target.value)}
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="phone">전화번호</Label>
            <Input
              id="phone"
              maxLength={20}
              placeholder="010-0000-0000"
              value={form.phone}
              onChange={(event) => change('phone', event.target.value)}
            />
          </div>

          {error !== null && <ErrorText message={error} />}

          <Button type="submit" disabled={pending}>
            {pending ? '가입 중…' : '회원가입'}
          </Button>

          <p className="text-muted-foreground text-center text-sm">
            이미 계정이 있으신가요?{' '}
            <Link to="/login" className="underline">
              로그인
            </Link>
          </p>
        </form>
      </CardContent>
    </Card>
  )
}
