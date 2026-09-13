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
import { ApiError } from '@/shared/api/client/client'
import { signUp } from '@/features/auth/api/endpoints/endpoints'
import type { SignUpRequest } from '@/features/auth/api/types/types'

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
      await signUp(form)
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
    <Page title="회원가입" description="차량 한 대만 있으면 바로 시작할 수 있습니다.">
      {/* 폼 + 아래 안내 문구는 한 덩어리다. 가로 폭(22rem)은 AuthLayout이 정한다. */}
      <div className="flex flex-col gap-6">
        <Card>
          <CardContent>
            <form className="flex flex-col gap-5" onSubmit={handleSubmit}>
              <Field label="이메일" htmlFor="email">
                <Input
                  id="email"
                  type="email"
                  required
                  maxLength={100}
                  autoComplete="email"
                  placeholder="you@example.com"
                  value={form.email}
                  onChange={(event) => change('email', event.target.value)}
                />
              </Field>

              {/* hint: 규칙을 에러로 알려주기 전에 미리 말해 준다. 틀린 뒤에 알려주는 건 늦다. */}
              <Field label="비밀번호" htmlFor="password" hint="8자 이상">
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
              </Field>

              <Field label="닉네임" htmlFor="nickname">
                <Input
                  id="nickname"
                  required
                  maxLength={30}
                  value={form.nickname}
                  onChange={(event) => change('nickname', event.target.value)}
                />
              </Field>

              <Field label="전화번호" htmlFor="phone" hint="선택 입력">
                <Input
                  id="phone"
                  maxLength={20}
                  placeholder="010-0000-0000"
                  value={form.phone}
                  onChange={(event) => change('phone', event.target.value)}
                />
              </Field>

              {error !== null && <ErrorText message={error} />}

              <FormActions>
                <Button type="submit" disabled={pending}>
                  {pending ? '가입 중…' : '회원가입'}
                </Button>
              </FormActions>
            </form>
          </CardContent>
        </Card>

        <p className="text-center text-sm text-muted-foreground">
          이미 계정이 있으신가요?{' '}
          <Link
            to="/login"
            className="text-strong transition-opacity duration-200 ease-apple hover:opacity-70"
          >
            로그인
          </Link>
        </p>
      </div>
    </Page>
  )
}
