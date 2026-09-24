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
import { passwordHint } from '@/shared/lib/limits/limits'
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

  // 상태 4개 대신 객체 하나. 바뀐 키만 덮어쓰기
  function change(key: keyof SignUpRequest, value: string) {
    setForm((previous) => ({ ...previous, [key]: value }))
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setPending(true)

    try {
      // 선택 입력이라 빈 값 미전송. '' 를 보내면 nullable 컬럼에 null 이 영영 안 생김
      // 공백만 친 것도 안 적은 것으로 처리
      const phone = form.phone?.trim()
      await signUp({ ...form, phone: phone === '' ? undefined : phone })
      // 가입 API 는 세션을 안 만들므로 로그인까지 이어서
      await login({ email: form.email, password: form.password })
      navigate('/vehicles', { replace: true })
    } catch (caught) {
      // 409 = 이메일 중복, 400 = 검증 실패. 둘 다 백엔드 메시지 그대로
      setError(caught instanceof ApiError ? caught.message : '회원가입에 실패했습니다.')
    } finally {
      setPending(false)
    }
  }

  return (
    <Page title="회원가입" description="차량 한 대만 있으면 바로 시작할 수 있습니다.">
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
                  maxLength={100}
                  autoComplete="email"
                  placeholder="you@example.com"
                  value={form.email}
                  onChange={(event) => change('email', event.target.value)}
                />
              </Field>

              {/* 규칙은 틀리기 전에 */}
              <Field label="비밀번호" htmlFor="password" hint={passwordHint(form.password)}>
                <Input
                  id="password"
                  type="password"
                  required
                  minLength={8}
                  // 글자 수 상한이라 한글 24자(=72바이트)는 못 막는다. 거친 천장일 뿐이고
                  // 실제 판정은 위 hint 와 서버의 @MaxBytes 가 한다
                maxLength={72}
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
