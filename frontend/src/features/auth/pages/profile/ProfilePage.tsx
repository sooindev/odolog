import { useState } from 'react'
import type { ChangeEvent, FormEvent } from 'react'

import { useAuth } from '@/features/auth/context/definition/AuthContext'
import { useTheme } from '@/shared/theme/context/ThemeContext'
import { ThemeToggle } from '@/shared/theme/toggle/ThemeToggle'
import { Button } from '@/shared/ui/base/button'
import { Card, CardContent } from '@/shared/ui/base/card'
import { Field } from '@/shared/ui/form/field'
import { Input } from '@/shared/ui/base/input'
import { FormActions, Page } from '@/shared/ui/layout/page'
import { Section } from '@/shared/ui/layout/section'
import {
  changePassword,
  exportAccount,
  restoreAccount,
} from '@/features/auth/api/endpoints/endpoints'
import { useNavigate } from 'react-router'
import { ErrorText, NoticeText } from '@/shared/ui/feedback/state'
import { ApiError } from '@/shared/api/client/client'
import { todayString } from '@/shared/lib/format/format'
import { passwordHint } from '@/shared/lib/limits/limits'
import { updateProfile } from '@/features/auth/api/endpoints/endpoints'
import type {
  AccountExport,
  AccountRestoreResult,
  UpdateProfileRequest,
  UserResponse,
} from '@/features/auth/api/types/types'

export function ProfilePage() {
  const { user } = useAuth()

  // null 은 여기서 거르고 폼에는 확정된 user 전달
  if (user === null) {
    return null
  }

  return (
    <Page eyebrow="Account" title="내 정보">
      <Section title="계정" description="닉네임과 전화번호를 바꿀 수 있습니다. 이메일은 변경할 수 없습니다.">
        <ProfileForm user={user} />
      </Section>

      {/* 계정 성격이라 화면 설정보다 앞 */}
      <Section
        title="비밀번호"
        description="바꾸려면 현재 비밀번호를 함께 입력해야 합니다. 변경해도 로그인은 유지됩니다."
      >
        <PasswordForm />
      </Section>

      {/* 헤더 토글과 같은 상태 공유. 여기서는 현재 설정 확인용 */}
      <Section title="화면" description="라이트·다크 중 하나를 고르거나, 기기 설정을 그대로 따를 수 있습니다.">
        <AppearanceCard />
      </Section>

      <Section
        title="내 기록"
        description="차량·정비 이력·주유 기록을 JSON 파일 하나로 내려받습니다. 비밀번호는 담기지 않습니다."
      >
        <ExportCard />
      </Section>

      {/* 되돌릴 수 없는 동작은 맨 아래 */}
      <Section
        title="회원 탈퇴"
        description="계정과 등록한 차량, 정비 이력과 주유 기록이 모두 삭제됩니다. 되돌릴 수 없습니다."
      >
        <WithdrawCard />
      </Section>
    </Page>
  )
}

function ExportCard() {
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  async function handleExport() {
    setError(null)
    setPending(true)

    try {
      const data = await exportAccount()

      // <a href> 대신 fetch 후 파일 생성. 세션·CSRF 헤더 유지
      const url = URL.createObjectURL(
        new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' }),
      )
      const link = document.createElement('a')
      link.href = url
      link.download = `odolog-${todayString()}.json`
      link.click()

      // revoke 는 다음 틱에. 즉시 해제 시 다운로드 취소·0바이트 파일
      setTimeout(() => URL.revokeObjectURL(url), 0)
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : '내보내기에 실패했습니다.')
    } finally {
      setPending(false)
    }
  }

  return (
    <Card>
      <CardContent className="flex flex-col gap-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <p className="min-w-0 text-caption text-muted-foreground">
            탈퇴하면 기록은 복구되지 않습니다. 지우기 전에 받아 두세요.
          </p>
          <Button variant="secondary" onClick={handleExport} disabled={pending} className="shrink-0">
            {pending ? '준비 중…' : 'JSON 내려받기'}
          </Button>
        </div>
        {error !== null && <ErrorText message={error} />}

        <div className="border-t border-border pt-4">
          <RestoreForm />
        </div>
      </CardContent>
    </Card>
  )
}

/**
 * 내보낸 파일 복원. 내보내기 바로 아래 배치
 * 브라우저에서 읽어 JSON 전송. multipart 미사용
 */
function RestoreForm() {
  const [result, setResult] = useState<AccountRestoreResult | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  async function handleFile(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    // 같은 파일 재선택 허용
    event.target.value = ''
    if (file === undefined) {
      return
    }

    setResult(null)
    setError(null)
    setPending(true)

    try {
      const parsed = JSON.parse(await file.text()) as { vehicles?: AccountExport['vehicles'] }
      if (!Array.isArray(parsed.vehicles)) {
        // 형식이 다른 파일은 전송 전 안내
        throw new SyntaxError('vehicles 없음')
      }

      setResult(await restoreAccount(parsed.vehicles))
    } catch (caught) {
      setError(
        caught instanceof SyntaxError
          ? '오도로그에서 내려받은 JSON 파일이 맞는지 확인해 주세요.'
          : caught instanceof ApiError
            ? caught.message
            : '가져오기에 실패했습니다.',
      )
    } finally {
      setPending(false)
    }
  }

  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <p className="min-w-0 text-caption text-muted-foreground">
          받아 둔 파일을 다시 넣습니다. 같은 기록은 건너뛰므로 두 번 넣어도 늘지 않습니다.
        </p>
        {/* label 로 감싼 버튼형 파일 입력. 기본 모양은 테마 미반영 */}
        <label className="shrink-0">
          <span
            className={
              'inline-flex h-9 cursor-pointer items-center border border-border bg-fill px-4 ' +
              'text-caption text-strong transition-colors duration-200 ease-apple hover:bg-card-hover'
            }
          >
            {pending ? '가져오는 중…' : 'JSON 가져오기'}
          </span>
          <input
            type="file"
            accept="application/json,.json"
            className="sr-only"
            disabled={pending}
            onChange={handleFile}
          />
        </label>
      </div>

      {/* 건너뛴 수까지 안내 */}
      {result !== null && (
        <NoticeText
          message={
            `차량 ${result.addedVehicles}대와 기록 ` +
            `${result.addedMaintenanceRecords + result.addedFuelRecords}건을 넣었습니다.` +
            (result.mergedVehicles > 0
              ? ` 이미 있던 차량 ${result.mergedVehicles}대에는 기록만 붙였습니다.`
              : '') +
            (result.addedServiceIntervals > 0
              ? ` 차량별 정비 주기 ${result.addedServiceIntervals}개도 되살렸습니다.`
              : '') +
            (result.skippedRecords > 0
              ? ` 이미 같은 기록이 있어 ${result.skippedRecords}건은 건너뛰었습니다.`
              : '')
          }
        />
      )}

      {error !== null && <ErrorText message={error} />}
    </div>
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

    // 확인란은 전송하지 않음
    if (newPassword !== confirmPassword) {
      setError('새 비밀번호가 서로 다릅니다.')
      return
    }

    setPending(true)

    try {
      await changePassword({ currentPassword, newPassword })
      setMessage('비밀번호를 변경했습니다.')
      // 성공 시 입력칸 비움
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
    } catch (caught) {
      // 401 = 현재 비밀번호 오류
      setError(caught instanceof ApiError ? caught.message : '비밀번호 변경에 실패했습니다.')
    } finally {
      setPending(false)
    }
  }

  return (
    <Card>
      <CardContent>
        <form className="flex flex-col gap-5" onSubmit={handleSubmit}>
          {/* autoComplete 로 비밀번호 관리자의 현재·새 비밀번호 구분 */}
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
            <Field label="새 비밀번호" htmlFor="new-password" hint={passwordHint(newPassword)}>
              <Input
                id="new-password"
                type="password"
                required
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
      // replace: 뒤로가기로 복귀 방지
      navigate('/', { replace: true })
    } catch (caught) {
      // 401 = 비밀번호 오류. 전역 401 처리 제외 경로
      setError(caught instanceof ApiError ? caught.message : '탈퇴에 실패했습니다.')
      // 성공 시 화면 이탈, 실패 시에만 복구
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
                {/* 본인 확인용 비밀번호 */}
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
            {/* min-w-0: 버튼 밀림 방지 */}
            <p className="min-w-0 text-caption text-muted-foreground">
              탈퇴하면 같은 이메일로 다시 가입할 수 있지만, 기록은 복구되지 않습니다.
            </p>
            {/* 채우지 않은 빨간 버튼 */}
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

  // system 일 때 현재 적용 모드 표시
  const detail =
    theme === 'system'
      ? `기기 설정을 따릅니다. 지금은 ${resolved === 'dark' ? '다크' : '라이트'}입니다.`
      : `${theme === 'dark' ? '다크' : '라이트'}로 고정되어 있습니다.`

  return (
    <Card>
      {/*
        좁은 화면은 세로 배치
        min-w-0: 토글 밀림 방지
      */}
      <CardContent className="flex flex-col gap-5 sm:flex-row sm:items-center sm:justify-between sm:gap-4">
        <div className="flex min-w-0 flex-col gap-1">
          <p className="text-body text-strong">화면 모드</p>
          <p className="text-caption text-muted-foreground">{detail}</p>
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

    // 바뀐 필드만
    const request: UpdateProfileRequest = {}
    if (nickname !== user.nickname) request.nickname = nickname
    if (phone !== (user.phone ?? '')) request.phone = phone

    if (Object.keys(request).length === 0) {
      setMessage('변경된 내용이 없습니다.')
      return
    }

    setPending(true)
    try {
      const updated = await updateProfile(request)
      replaceUser(updated)
      // 입력칸도 서버 저장값으로. 공백 정리 후 재전송 방지
      setNickname(updated.nickname)
      setPhone(updated.phone ?? '')
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
          {/* 이메일은 표시만 */}
          <Field label="이메일" htmlFor="email">
            <Input id="email" value={user.email} disabled />
          </Field>

          {/* 넓은 화면은 두 칸 나란히 */}
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
