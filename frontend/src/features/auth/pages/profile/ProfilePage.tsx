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
import { changePassword } from '@/features/auth/api/endpoints/endpoints'
import { useNavigate } from 'react-router'
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

      {/* 계정 바로 다음에 둔다. 화면 설정보다 앞이어야 하는 이유는 성격이 계정 쪽이기 때문이다. */}
      <Section
        title="비밀번호"
        description="바꾸려면 현재 비밀번호를 함께 입력해야 합니다. 변경해도 로그인은 유지됩니다."
      >
        <PasswordForm />
      </Section>

      {/*
        화면 모드를 여기에 둔 이유: 헤더의 컨트롤은 "지금 당장 바꾸는" 자리이고,
        설정 화면은 "이 앱이 무엇을 기억하고 있는지" 확인하는 자리다.
        같은 컴포넌트를 두 곳에서 쓰지만 상태가 하나라 어긋날 일은 없다.
      */}
      <Section title="화면" description="라이트·다크 중 하나를 고르거나, 기기 설정을 그대로 따를 수 있습니다.">
        <AppearanceCard />
      </Section>

      {/* 되돌릴 수 없는 동작은 맨 아래에. 위에 두면 스크롤할 때마다 지나치게 된다. */}
      <Section
        title="회원 탈퇴"
        description="계정과 등록한 차량·정비 이력이 모두 삭제됩니다. 되돌릴 수 없습니다."
      >
        <WithdrawCard />
      </Section>
    </Page>
  )
}

function PasswordForm() {
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setMessage(null)
    setError(null)

    // 확인란은 서버에 보내지 않는다. "두 번 같게 쳤는가"는 오타 방지 장치일 뿐이라
    // 서버가 알 이유가 없고, 알면 비밀번호를 한 번 더 전송하는 셈이 된다.
    if (newPassword !== confirmPassword) {
      setError('새 비밀번호가 서로 다릅니다.')
      return
    }

    setPending(true)

    try {
      await changePassword({ currentPassword, newPassword })
      setMessage('비밀번호를 변경했습니다.')
      // 성공하면 비운다. 화면에 남겨 두면 다음 사람이 그대로 볼 수 있다.
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
    } catch (caught) {
      // 401 은 현재 비밀번호가 틀린 경우. 여기서는 이미 본인이 로그인한 상태라
      // 로그인 화면처럼 사유를 뭉뚱그릴 이유가 없다.
      setError(caught instanceof ApiError ? caught.message : '비밀번호 변경에 실패했습니다.')
    } finally {
      setPending(false)
    }
  }

  return (
    <Card>
      <CardContent>
        <form className="flex flex-col gap-5" onSubmit={handleSubmit}>
          {/* autoComplete 을 정확히 적어야 비밀번호 관리자가 "현재"와 "새것"을 구분한다.
              전부 password 로 두면 저장된 값이 새 비밀번호 칸에 채워진다. */}
          <Field label="현재 비밀번호" htmlFor="current-password">
            <Input
              id="current-password"
              type="password"
              required
              autoComplete="current-password"
              value={currentPassword}
              onChange={(event) => setCurrentPassword(event.target.value)}
            />
          </Field>

          <div className="grid gap-5 sm:grid-cols-2">
            <Field label="새 비밀번호" htmlFor="new-password" hint="8자 이상">
              <Input
                id="new-password"
                type="password"
                required
                minLength={8}
                maxLength={100}
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
          </div>

          {message !== null && <NoticeText message={message} />}
          {error !== null && <ErrorText message={error} />}

          <FormActions>
            <Button type="submit" disabled={pending}>
              {pending ? '변경 중…' : '비밀번호 변경'}
            </Button>
          </FormActions>
        </form>
      </CardContent>
    </Card>
  )
}

function WithdrawCard() {
  const { withdraw } = useAuth()
  const navigate = useNavigate()

  const [open, setOpen] = useState(false)
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setPending(true)

    try {
      await withdraw({ password })
      // replace: true — 뒤로가기로 방금 떠난 화면에 돌아가지 못하게 한다.
      navigate('/', { replace: true })
    } catch (caught) {
      // 401 은 비밀번호가 틀린 것. 이 경로는 전역 401 핸들러에서 제외돼 있어
      // 여기서 잡아 보여줄 수 있다 (안 그러면 그냥 로그아웃돼 버린다).
      setError(caught instanceof ApiError ? caught.message : '탈퇴에 실패했습니다.')
      // 성공하면 화면을 떠나므로 실패했을 때만 되돌린다.
      setPending(false)
    }
  }

  return (
    <Card>
      <CardContent>
        {open ? (
          <div className="form-open">
            <div>
              <form className="flex flex-col gap-5" onSubmit={handleSubmit}>
                {/* 확인 문구를 따라 치게 하는 방식도 있지만 비밀번호가 더 강한 관문이다.
                    그건 실수만 막고, 이건 본인인지까지 확인한다. */}
                <Field
                  label="비밀번호"
                  htmlFor="withdraw-password"
                  hint="본인 확인을 위해 현재 비밀번호를 입력하세요."
                >
                  <Input
                    id="withdraw-password"
                    type="password"
                    required
                    autoComplete="current-password"
                    value={password}
                    onChange={(event) => setPassword(event.target.value)}
                  />
                </Field>

                {error !== null && <ErrorText message={error} />}

                <FormActions>
                  <Button type="submit" variant="destructive" disabled={pending}>
                    {pending ? '탈퇴 중…' : '탈퇴하기'}
                  </Button>
                  <Button
                    type="button"
                    variant="ghost"
                    onClick={() => {
                      setOpen(false)
                      setPassword('')
                      setError(null)
                    }}
                  >
                    취소
                  </Button>
                </FormActions>
              </form>
            </div>
          </div>
        ) : (
          <div className="flex flex-wrap items-center justify-between gap-3">
            {/* min-w-0: 글이 줄어들지 못하면 버튼을 아래로 밀어낸다. */}
            <p className="min-w-0 text-[0.8125rem] text-muted-foreground">
              탈퇴하면 같은 이메일로 다시 가입할 수 있지만, 기록은 복구되지 않습니다.
            </p>
            {/* 빨갛게 채우지 않는다. 가장 하면 안 되는 일이 화면에서 가장 강한 요소가 된다. */}
            <Button
              variant="destructive"
              size="sm"
              className="shrink-0"
              onClick={() => setOpen(true)}
            >
              회원 탈퇴
            </Button>
          </div>
        )}
      </CardContent>
    </Card>
  )
}

function AppearanceCard() {
  const { theme, resolved } = useTheme()

  // 'system' 일 때 지금 실제로 어느 쪽인지. 없으면 "시스템 설정" 이라고만 적힌다.
  const detail =
    theme === 'system'
      ? `기기 설정을 따릅니다. 지금은 ${resolved === 'dark' ? '다크' : '라이트'}입니다.`
      : `${theme === 'dark' ? '다크' : '라이트'}로 고정되어 있습니다.`

  return (
    <Card>
      {/*
        좁은 화면에서는 세로로 쌓는다. 한 줄에 두면 남는 폭이 설명 문구 길이에 좌우되는데,
        'system' 일 때 문구가 가장 길어서 그때만 토글이 아래로 밀려 내려갔다.
        min-w-0: 글 덩어리가 줄어들 수 있어야 토글을 밀어내지 않는다.
      */}
      <CardContent className="flex flex-col gap-5 sm:flex-row sm:items-center sm:justify-between sm:gap-4">
        <div className="flex min-w-0 flex-col gap-1">
          <p className="text-[0.9375rem] tracking-[-0.01em] text-strong">화면 모드</p>
          <p className="text-[0.8125rem] text-muted-foreground">{detail}</p>
        </div>
        <ThemeToggle className="shrink-0" />
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
