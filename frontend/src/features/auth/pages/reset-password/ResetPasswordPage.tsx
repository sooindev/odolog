import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router'

import { confirmPasswordReset } from '@/features/auth/api/endpoints/endpoints'
import { ApiError } from '@/shared/api/client/client'
import { Button } from '@/shared/ui/base/button'
import { Card, CardContent } from '@/shared/ui/base/card'
import { Input } from '@/shared/ui/base/input'
import { ErrorText } from '@/shared/ui/feedback/state'
import { Field } from '@/shared/ui/form/field'
import { FormActions, Page } from '@/shared/ui/layout/page'
import { passwordHint } from '@/shared/lib/limits/limits'

export function ResetPasswordPage() {
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const token = params.get('token')

  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)

    // 확인란은 전송하지 않음
    if (newPassword !== confirmPassword) {
      setError('새 비밀번호가 서로 다릅니다.')
      return
    }

    setPending(true)

    try {
      await confirmPasswordReset({ token: token ?? '', newPassword })
      // 자동 로그인 없이 로그인 화면으로. 안내는 state 로 전달(새로고침 시 사라짐)
      navigate('/login', {
        replace: true,
        state: { notice: '비밀번호를 바꿨습니다. 새 비밀번호로 로그인해 주세요.' },
      })
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : '비밀번호 재설정에 실패했습니다.')
    } finally {
      setPending(false)
    }
  }

  // 토큰 없이 직접 들어온 경우
  if (token === null || token === '') {
    return (
      <Page title="새 비밀번호" description="메일로 받은 링크에서만 들어올 수 있습니다.">
        <Card>
          <CardContent className="flex flex-col gap-4">
            <ErrorText message="재설정 링크가 올바르지 않습니다." />
            <p className="text-caption leading-relaxed text-muted-foreground">
              메일에 있는 링크를 그대로 눌러 주세요. 링크가 만료됐다면 다시 요청할 수 있습니다.
            </p>
            <FormActions>
              <Button render={<Link to="/forgot-password" />}>재설정 링크 다시 받기</Button>
            </FormActions>
          </CardContent>
        </Card>
      </Page>
    )
  }

  return (
    <Page title="새 비밀번호" description="8자 이상으로 정해 주세요. 바꾼 뒤 다시 로그인합니다.">
      <Card>
        <CardContent>
          <form className="flex flex-col gap-5" onSubmit={handleSubmit}>
          <Field label="새 비밀번호" htmlFor="new-password" hint={passwordHint(newPassword)}>
            <Input
              id="new-password"
              type="password"
              required
              autoFocus
              minLength={8}
              // 글자 수 상한은 대략적인 천장. 실제 판정은 hint 와 서버 @MaxBytes
              maxLength={72}
              autoComplete="new-password"
              value={newPassword}
              onChange={(event) => setNewPassword(event.target.value)}
            />
          </Field>

          <Field label="새 비밀번호 확인" htmlFor="confirm-password">
            <Input
              id="confirm-password"
              type="password"
              required
              autoComplete="new-password"
              value={confirmPassword}
              onChange={(event) => setConfirmPassword(event.target.value)}
            />
          </Field>

          {error !== null && <ErrorText message={error} />}

          <FormActions>
            <Button type="submit" disabled={pending}>
              {pending ? '변경 중…' : '비밀번호 변경'}
            </Button>
          </FormActions>
          </form>
        </CardContent>
      </Card>
    </Page>
  )
}
