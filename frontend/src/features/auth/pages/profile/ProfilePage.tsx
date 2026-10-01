import { useMemo, useState } from 'react'
import type { ChangeEvent, FormEvent } from 'react'
import { useNavigate } from 'react-router'
import { ChevronDown } from 'lucide-react'
import { cn } from 'cn'

import { useAuth } from '@/features/auth/context/definition/AuthContext'
import { useTheme } from '@/shared/theme/context/ThemeContext'
import { ThemeToggle } from '@/shared/theme/toggle/ThemeToggle'
import { Button } from '@/shared/ui/base/button'
import { Card, CardContent } from '@/shared/ui/base/card'
import { Field } from '@/shared/ui/form/field'
import { controlClassName } from '@/shared/ui/form/control'
import { Input } from '@/shared/ui/base/input'
import { FormActions, Page } from '@/shared/ui/layout/page'
import { Section } from '@/shared/ui/layout/section'
import {
  changePassword,
  exportAccount,
  restoreAccount,
  updateProfile,
} from '@/features/auth/api/endpoints/endpoints'
import { ErrorText, NoticeText } from '@/shared/ui/feedback/state'
import { useI18n } from '@/shared/i18n/context/I18nContext'
import { errorMessage } from '@/shared/i18n/errors/errorMessage'
import { todayString } from '@/shared/lib/format/format'
import { passwordHint } from '@/shared/lib/limits/limits'
import type { Language, UnitSystem } from '@/shared/lib/locale/preferences'
import type {
  AccountExport,
  AccountRestoreResult,
  UpdateProfileRequest,
  UserResponse,
} from '@/features/auth/api/types/types'

export function ProfilePage() {
  const { user } = useAuth()
  const { t } = useI18n()

  // null 은 여기서 거르고 폼에는 확정된 user 전달
  if (user === null) {
    return null
  }

  return (
    <Page eyebrow="Account" title={t.profile.title}>
      <Section title={t.profile.account.title} description={t.profile.account.description}>
        <ProfileForm user={user} />
      </Section>

      {/* 계정 성격이라 화면 설정보다 앞 */}
      <Section title={t.profile.password.title} description={t.profile.password.description}>
        <PasswordForm />
      </Section>

      {/* 저장하면 화면 언어·단위가 바로 바뀜 */}
      <Section title={t.profile.region.title} description={t.profile.region.description}>
        <RegionForm user={user} />
      </Section>

      {/* 헤더 토글과 같은 상태 공유. 여기서는 현재 설정 확인용 */}
      <Section title={t.profile.appearance.title} description={t.profile.appearance.description}>
        <AppearanceCard />
      </Section>

      <Section title={t.profile.data.title} description={t.profile.data.description}>
        <ExportCard />
      </Section>

      {/* 되돌릴 수 없는 동작은 맨 아래 */}
      <Section title={t.profile.withdraw.title} description={t.profile.withdraw.description}>
        <WithdrawCard />
      </Section>
    </Page>
  )
}

function ExportCard() {
  const { t, timeZone } = useI18n()
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
      link.download = `odolog-${todayString(timeZone)}.json`
      link.click()

      // revoke 는 다음 틱에. 즉시 해제 시 다운로드 취소·0바이트 파일
      setTimeout(() => URL.revokeObjectURL(url), 0)
    } catch (caught) {
      setError(errorMessage(caught, t, t.profile.data.exportFailed))
    } finally {
      setPending(false)
    }
  }

  return (
    <Card>
      <CardContent className="flex flex-col gap-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <p className="min-w-0 text-caption text-muted-foreground">{t.profile.data.exportNote}</p>
          <Button variant="secondary" onClick={handleExport} disabled={pending} className="shrink-0">
            {pending ? t.profile.data.exporting : t.profile.data.export}
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
  const { t } = useI18n()
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
        throw new SyntaxError('vehicles missing')
      }

      setResult(await restoreAccount(parsed.vehicles))
    } catch (caught) {
      setError(
        caught instanceof SyntaxError
          ? t.profile.data.notOurFile
          : errorMessage(caught, t, t.profile.data.importFailed),
      )
    } finally {
      setPending(false)
    }
  }

  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <p className="min-w-0 text-caption text-muted-foreground">{t.profile.data.importNote}</p>
        {/* label 로 감싼 버튼형 파일 입력. 기본 모양은 테마 미반영 */}
        {/* 입력이 span 앞에 있어야 peer 로 포커스를 그릴 수 있음. 입력 자체는 sr-only 라 안 보임 */}
        <label className="shrink-0">
          <input
            type="file"
            accept="application/json,.json"
            className="peer sr-only"
            disabled={pending}
            onChange={handleFile}
          />
          <span
            className={
              'inline-flex h-9 cursor-pointer items-center border border-border bg-fill px-4 ' +
              'text-caption text-strong transition-colors duration-200 ease-apple hover:bg-card-hover ' +
              'peer-focus-visible:ring-2 peer-focus-visible:ring-ring peer-focus-visible:ring-offset-2 ' +
              'peer-focus-visible:ring-offset-background'
            }
          >
            {pending ? t.profile.data.importing : t.profile.data.import}
          </span>
        </label>
      </div>

      {/* 건너뛴 수까지 안내 */}
      {result !== null && (
        <NoticeText
          message={
            t.profile.data.imported(
              result.addedVehicles,
              result.addedMaintenanceRecords + result.addedFuelRecords,
            ) +
            (result.mergedVehicles > 0 ? t.profile.data.merged(result.mergedVehicles) : '') +
            (result.addedServiceIntervals > 0 ? t.profile.data.intervals(result.addedServiceIntervals) : '') +
            (result.skippedRecords > 0 ? t.profile.data.skipped(result.skippedRecords) : '')
          }
        />
      )}

      {error !== null && <ErrorText message={error} />}
    </div>
  )
}

function PasswordForm() {
  const { t } = useI18n()
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
      setError(t.password.mismatch)
      return
    }

    setPending(true)

    try {
      await changePassword({ currentPassword, newPassword })
      setMessage(t.profile.password.changed)
      // 성공 시 입력칸 비움
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
    } catch (caught) {
      // 401 = 현재 비밀번호 오류
      setError(errorMessage(caught, t, t.profile.password.failed))
    } finally {
      setPending(false)
    }
  }

  return (
    <Card>
      <CardContent>
        <form className="flex flex-col gap-5" onSubmit={handleSubmit}>
          {/* autoComplete 로 비밀번호 관리자의 현재·새 비밀번호 구분 */}
          <Field label={t.password.current} htmlFor="current-password">
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
            <Field label={t.password.newPassword} htmlFor="new-password" hint={passwordHint(newPassword, t)}>
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

            <Field label={t.password.confirm} htmlFor="confirm-password">
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
              {pending ? t.profile.password.submitting : t.profile.password.submit}
            </Button>
          </FormActions>
        </form>
      </CardContent>
    </Card>
  )
}

function WithdrawCard() {
  const { withdraw } = useAuth()
  const { t } = useI18n()
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
      setError(errorMessage(caught, t, t.profile.withdraw.failed))
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
                  label={t.common.password}
                  htmlFor="withdraw-password"
                  hint={t.profile.withdraw.passwordHint}
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
                    {pending ? t.profile.withdraw.submitting : t.profile.withdraw.submit}
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
                    {t.common.cancel}
                  </Button>
                </FormActions>
              </form>
            </div>
          </div>
        ) : (
          <div className="flex flex-wrap items-center justify-between gap-3">
            {/* min-w-0: 버튼 밀림 방지 */}
            <p className="min-w-0 text-caption text-muted-foreground">{t.profile.withdraw.note}</p>
            {/* 채우지 않은 빨간 버튼 */}
            <Button
              variant="destructive"
              size="sm"
              className="shrink-0"
              onClick={() => setOpen(true)}
            >
              {t.profile.withdraw.open}
            </Button>
          </div>
        )}
      </CardContent>
    </Card>
  )
}

function AppearanceCard() {
  const { theme, resolved } = useTheme()
  const { t } = useI18n()

  // system 일 때 현재 적용 모드 표시
  const detail =
    theme === 'system'
      ? t.profile.appearance.followsSystem(resolved === 'dark')
      : t.profile.appearance.fixed(theme === 'dark')

  return (
    <Card>
      {/*
        좁은 화면은 세로 배치
        min-w-0: 토글 밀림 방지
      */}
      <CardContent className="flex flex-col gap-5 sm:flex-row sm:items-center sm:justify-between sm:gap-4">
        <div className="flex min-w-0 flex-col gap-1">
          <p className="text-body text-strong">{t.profile.appearance.mode}</p>
          <p className="text-caption text-muted-foreground">{detail}</p>
        </div>
        <ThemeToggle className="shrink-0" />
      </CardContent>
    </Card>
  )
}

function ProfileForm({ user }: { user: UserResponse }) {
  const { replaceUser } = useAuth()
  const { t } = useI18n()

  const [nickname, setNickname] = useState(user.nickname)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setMessage(null)
    setError(null)

    if (nickname === user.nickname) {
      setMessage(t.common.noChanges)
      return
    }

    setPending(true)
    try {
      const updated = await updateProfile({ nickname })
      replaceUser(updated)
      // 입력칸도 서버 저장값으로. 공백 정리 후 재전송 방지
      setNickname(updated.nickname)
      setMessage(t.common.saved)
    } catch (caught) {
      setError(errorMessage(caught, t, t.profile.account.failed))
    } finally {
      setPending(false)
    }
  }

  return (
    <Card>
      <CardContent>
        <form className="flex flex-col gap-5" onSubmit={handleSubmit}>
          {/* 넓은 화면은 두 칸 나란히. 이메일은 표시만 */}
          <div className="grid gap-5 sm:grid-cols-2">
            <Field label={t.common.email} htmlFor="email">
              <Input id="email" value={user.email} disabled />
            </Field>

            <Field label={t.common.nickname} htmlFor="nickname">
              <Input
                id="nickname"
                required
                maxLength={30}
                value={nickname}
                onChange={(event) => setNickname(event.target.value)}
              />
            </Field>
          </div>

          {message !== null && <NoticeText message={message} />}
          {error !== null && <ErrorText message={error} />}

          <FormActions>
            <Button type="submit" disabled={pending}>
              {pending ? t.common.saving : t.common.save}
            </Button>
          </FormActions>
        </form>
      </CardContent>
    </Card>
  )
}

const LANGUAGES: Language[] = ['KO', 'EN']
const UNIT_SYSTEMS: UnitSystem[] = ['KM_PER_L', 'L_PER_100KM', 'MPG_US', 'MPG_UK']

/** 목록은 브라우저가 아는 값 전부. 지금 값이 목록에 없으면 앞에 붙임(UTC 등) */
function withCurrent(values: string[], current: string) {
  return values.includes(current) ? values : [current, ...values]
}

/** 언어·단위·통화·시간대. 저장하면 I18nProvider 가 새 값으로 화면 전체를 다시 그림 */
function RegionForm({ user }: { user: UserResponse }) {
  const { replaceUser } = useAuth()
  const { t, locale } = useI18n()

  const [language, setLanguage] = useState(user.language)
  const [unitSystem, setUnitSystem] = useState(user.unitSystem)
  const [currency, setCurrency] = useState(user.currency)
  const [timeZone, setTimeZone] = useState(user.timeZone)
  const [message, setMessage] = useState<string | null>(null)
  // 문구 대신 표시 여부. 언어를 바꾼 저장이면 저장 직후의 새 언어로 보여야 함
  const [saved, setSaved] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  const browserTimeZone = Intl.DateTimeFormat().resolvedOptions().timeZone
  const currencies = useMemo(() => {
    const names = new Intl.DisplayNames([locale], { type: 'currency' })
    return withCurrent(Intl.supportedValuesOf('currency'), user.currency).map((code) => ({
      code,
      label: `${code} · ${names.of(code) ?? code}`,
    }))
  }, [locale, user.currency])
  const timeZones = useMemo(
    () => withCurrent(Intl.supportedValuesOf('timeZone'), user.timeZone),
    [user.timeZone],
  )

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setMessage(null)
    setSaved(false)
    setError(null)

    // 바뀐 필드만
    const request: UpdateProfileRequest = {}
    if (language !== user.language) request.language = language
    if (unitSystem !== user.unitSystem) request.unitSystem = unitSystem
    if (currency !== user.currency) request.currency = currency
    if (timeZone !== user.timeZone) request.timeZone = timeZone

    if (Object.keys(request).length === 0) {
      setMessage(t.common.noChanges)
      return
    }

    setPending(true)
    try {
      replaceUser(await updateProfile(request))
      setSaved(true)
    } catch (caught) {
      setError(errorMessage(caught, t, t.profile.region.failed))
    } finally {
      setPending(false)
    }
  }

  return (
    <Card>
      <CardContent>
        <form className="flex flex-col gap-5" onSubmit={handleSubmit}>
          <div className="grid gap-5 sm:grid-cols-2">
            <Field label={t.profile.region.language} htmlFor="language">
              <NativeSelect id="language" value={language} onChange={(next) => setLanguage(next as Language)}>
                {LANGUAGES.map((code) => (
                  <option key={code} value={code}>
                    {t.languages[code]}
                  </option>
                ))}
              </NativeSelect>
            </Field>

            <Field label={t.profile.region.unitSystem} htmlFor="unit-system">
              <NativeSelect
                id="unit-system"
                value={unitSystem}
                onChange={(next) => setUnitSystem(next as UnitSystem)}
              >
                {UNIT_SYSTEMS.map((system) => (
                  <option key={system} value={system}>
                    {t.units[system]}
                  </option>
                ))}
              </NativeSelect>
            </Field>

            {/* 이미 적은 기록은 원래 통화 그대로. 바꾸기 전에 알게 */}
            <Field label={t.profile.region.currency} htmlFor="currency" hint={t.profile.region.currencyHint}>
              <NativeSelect id="currency" value={currency} onChange={setCurrency}>
                {currencies.map(({ code, label }) => (
                  <option key={code} value={code}>
                    {label}
                  </option>
                ))}
              </NativeSelect>
            </Field>

            <Field
              label={t.profile.region.timeZone}
              htmlFor="time-zone"
              hint={t.profile.region.timeZoneHint(browserTimeZone)}
            >
              <NativeSelect id="time-zone" value={timeZone} onChange={setTimeZone}>
                {timeZones.map((zone) => (
                  <option key={zone} value={zone}>
                    {zone}
                  </option>
                ))}
              </NativeSelect>
            </Field>
          </div>

          {message !== null && <NoticeText message={message} />}
          {saved && <NoticeText message={t.common.saved} />}
          {error !== null && <ErrorText message={error} />}

          <FormActions>
            <Button type="submit" disabled={pending}>
              {pending ? t.common.saving : t.common.save}
            </Button>
          </FormActions>
        </form>
      </CardContent>
    </Card>
  )
}

/** 네이티브 select + 같은 톤 화살표. 정비 폼과 같은 모양 */
function NativeSelect({
  id,
  value,
  onChange,
  children,
}: {
  id: string
  value: string
  onChange: (value: string) => void
  children: React.ReactNode
}) {
  return (
    <div className="relative">
      <select
        id={id}
        className={cn(controlClassName, 'appearance-none pr-10')}
        value={value}
        onChange={(event) => onChange(event.target.value)}
      >
        {children}
      </select>
      <ChevronDown
        className="pointer-events-none absolute top-1/2 right-3.5 size-4 -translate-y-1/2 text-muted-foreground"
        aria-hidden="true"
      />
    </div>
  )
}
