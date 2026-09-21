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

    // 확인란은 서버로 보내지 않는다. 오타 방지 장치일 뿐이다
    if (newPassword !== confirmPassword) {
      setError('새 비밀번호가 서로 다릅니다.')
      return
    }

    setPending(true)

    try {
      await confirmPasswordReset({ token: token ?? '', newPassword })
      // 바꾼 비밀번호로 직접 로그인하게 한다 — 여기서 자동 로그인시키면
      // 메일 링크를 누른 사람이 곧 계정 주인이라고 믿는 셈이 된다
      navigate('/login', { replace: true })
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : '비밀번호 재설정에 실패했습니다.')
    } finally {
      setPending(false)
    }
  }

  // 토큰 없이 들어온 경우. 주소를 직접 친 사람이다
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
          <Field label="새 비밀번호" htmlFor="new-password" hint="8자 이상 · 한글은 24자까지">
            <Input
              id="new-password"
              type="password"
              required
              autoFocus
              minLength={8}
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
