import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router'

import { requestPasswordReset } from '@/features/auth/api/endpoints/endpoints'
import { ApiError } from '@/shared/api/client/client'
import { Button } from '@/shared/ui/base/button'
import { Card, CardContent } from '@/shared/ui/base/card'
import { Input } from '@/shared/ui/base/input'
import { ErrorText, NoticeText } from '@/shared/ui/feedback/state'
import { Field } from '@/shared/ui/form/field'
import { FormActions, Page } from '@/shared/ui/layout/page'

export function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [sent, setSent] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setPending(true)

    try {
      await requestPasswordReset({ email })
      setSent(true)
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : '요청에 실패했습니다.')
    } finally {
      setPending(false)
    }
  }

  return (
    // eyebrow 를 비운다 — 로그인 전에는 아직 아무 데도 속하지 않는다
    <Page title="비밀번호 재설정" description="가입할 때 쓴 주소로 재설정 링크를 보냅니다.">
      <div className="flex flex-col gap-6">
        <Card>
          <CardContent>
            {sent ? (
            <div className="flex flex-col gap-4">
              {/* 가입된 주소인지 알려 주지 않는다 — 알려 주면 가입 여부 조회가 된다 */}
              <NoticeText message="가입된 주소라면 재설정 링크를 보냈습니다. 메일함을 확인해 주세요." />
              <p className="text-caption leading-relaxed text-muted-foreground">
                링크는 30분 동안만 쓸 수 있고, 한 번 쓰면 사라집니다. 메일이 오지 않으면
                스팸함을 확인하거나 잠시 후 다시 요청해 주세요.
              </p>
            </div>
          ) : (
            <form className="flex flex-col gap-5" onSubmit={handleSubmit}>
              <Field
                label="이메일"
                htmlFor="email"
                hint="가입할 때 쓴 주소로 재설정 링크를 보냅니다."
              >
                <Input
                  id="email"
                  type="email"
                  required
                  autoFocus
                  maxLength={100}
                  autoComplete="email"
                  placeholder="you@example.com"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                />
              </Field>

              {error !== null && <ErrorText message={error} />}

              <FormActions>
                <Button type="submit" disabled={pending}>
                  {pending ? '보내는 중…' : '재설정 링크 받기'}
                </Button>
              </FormActions>
            </form>
          )}
        </CardContent>
      </Card>

      <p className="text-center text-caption text-muted-foreground">
        <Link
          to="/login"
          className="text-strong transition-opacity duration-200 ease-apple hover:opacity-70"
        >
          로그인으로 돌아가기
          </Link>
        </p>
      </div>
    </Page>
  )
}
