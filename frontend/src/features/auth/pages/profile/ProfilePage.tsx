import { useState } from 'react'
import type { FormEvent } from 'react'

import { useAuth } from '@/features/auth/context/definition/AuthContext'
import { useTheme } from '@/shared/theme/context/ThemeContext'
import { ThemeToggle } from '@/shared/theme/toggle/ThemeToggle'
import { Button } from '@/shared/ui/base/button'
import { Card, CardContent } from '@/shared/ui/base/card'
import { Field } from '@/shared/ui/form/field'
import { Input } from '@/shared/ui/base/input'
import { FormActions, Page } from '@/shared/ui/layout/page'
import { Section } from '@/shared/ui/layout/section'
import { ErrorText, NoticeText } from '@/shared/ui/feedback/state'
import { ApiError } from '@/shared/api/client/client'
import { updateProfile } from '@/features/auth/api/endpoints/endpoints'
import type { UpdateProfileRequest, UserResponse } from '@/features/auth/api/types/types'

export function ProfilePage() {
  const { user } = useAuth()

  // 여기서 null 을 걸러내고 폼에는 확정된 user 를 넘긴다. 폼 안에서 다시 따질 필요가 없다.
  if (user === null) {
    return null
  }

  return (
    <Page eyebrow="Account" title="내 정보">
      <Section title="계정" description="닉네임과 전화번호를 바꿀 수 있습니다. 이메일은 변경할 수 없습니다.">
        <ProfileForm user={user} />
      </Section>

      {/*
        화면 모드를 여기에 둔 이유: 헤더의 컨트롤은 "지금 당장 바꾸는" 자리이고,
        설정 화면은 "이 앱이 무엇을 기억하고 있는지" 확인하는 자리다.
        같은 컴포넌트를 두 곳에서 쓰지만 상태가 하나라 어긋날 일은 없다.
      */}
      <Section title="화면" description="라이트·다크 중 하나를 고르거나, 기기 설정을 그대로 따를 수 있습니다.">
        <AppearanceCard />
      </Section>
    </Page>
  )
}

function AppearanceCard() {
  const { theme, resolved } = useTheme()

  // 'system' 일 때 지금 실제로 어느 쪽인지. 없으면 "시스템 설정" 이라고만 적힌다.
  const detail =
    theme === 'system'
      ? `기기 설정을 따릅니다 — 현재 ${resolved === 'dark' ? '다크모드입니다' : '라이트모드입니다'}`
      : `${theme === 'dark' ? '다크' : '라이트'}로 고정되어 있습니다`

  return (
    <Card>
      <CardContent className="flex flex-wrap items-center justify-between gap-4">
        <div className="flex flex-col gap-1">
          <p className="text-[0.9375rem] tracking-[-0.01em] text-strong">화면 모드</p>
          <p className="text-[0.8125rem] text-muted-foreground">{detail}</p>
        </div>
        <ThemeToggle />
      </CardContent>
    </Card>
  )
}

function ProfileForm({ user }: { user: UserResponse }) {
  const { replaceUser } = useAuth()

  const [nickname, setNickname] = useState(user.nickname)
  const [phone, setPhone] = useState(user.phone ?? '')
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setMessage(null)
    setError(null)

    // 백엔드가 보낸 필드만 반영하므로 바뀐 것만 담는다.
    const request: UpdateProfileRequest = {}
    if (nickname !== user.nickname) request.nickname = nickname
    if (phone !== (user.phone ?? '')) request.phone = phone

    if (Object.keys(request).length === 0) {
      setMessage('변경된 내용이 없습니다.')
      return
    }

    setPending(true)
    try {
      replaceUser(await updateProfile(request))
      setMessage('저장했습니다.')
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : '저장에 실패했습니다.')
    } finally {
      setPending(false)
    }
  }

  return (
    <Card>
      <CardContent>
        <form className="flex flex-col gap-5" onSubmit={handleSubmit}>
          {/* 이메일은 수정 API가 없다. 보여주기만 한다. */}
          <Field label="이메일" htmlFor="email">
            <Input id="email" value={user.email} disabled />
          </Field>

          {/* 닉네임·전화번호는 짧은 값이라 넓은 화면에서 나란히 둔다. 세로로 쌓으면
              오른쪽이 통째로 비어서, 폼이 실제 하는 일보다 길어 보인다. */}
          <div className="grid gap-5 sm:grid-cols-2">
            <Field label="닉네임" htmlFor="nickname">
              <Input
                id="nickname"
                required
                maxLength={30}
                value={nickname}
                onChange={(event) => setNickname(event.target.value)}
              />
            </Field>

            <Field label="전화번호" htmlFor="phone">
              <Input
                id="phone"
                maxLength={20}
                placeholder="010-0000-0000"
                value={phone}
                onChange={(event) => setPhone(event.target.value)}
              />
            </Field>
          </div>

          {message !== null && <NoticeText message={message} />}
          {error !== null && <ErrorText message={error} />}

          <FormActions>
            <Button type="submit" disabled={pending}>
              {pending ? '저장 중…' : '저장'}
            </Button>
          </FormActions>
        </form>
      </CardContent>
    </Card>
  )
}
