import { useMemo, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import type { FormEvent } from 'react'

import { updateProfile } from '@/features/account/api/endpoints'
import type { UpdateProfileRequest } from '@/features/account/api/types'
import type { UserResponse } from '@/features/auth/api/types'
import { useAuth } from '@/features/auth/context/AuthContext'
import { Button } from '@/shared/ui/base/button'
import { Card, CardContent } from '@/shared/ui/base/card'
import { Field } from '@/shared/ui/form/field'
import { NativeSelect } from '@/shared/ui/form/native-select'
import { FormActions } from '@/shared/ui/layout/page'
import { ErrorText, NoticeText } from '@/shared/ui/state'
import { useI18n } from '@/shared/i18n/I18nContext'
import { errorMessage } from '@/shared/i18n/errorMessage'
import { queryKeys } from '@/shared/api/queryKeys'
import type { Language, UnitSystem } from '@/shared/lib/preferences'

const LANGUAGES: Language[] = ['KO', 'EN']
const UNIT_SYSTEMS: UnitSystem[] = ['KM_PER_L', 'L_PER_100KM', 'MPG_US', 'MPG_UK']

/** 목록은 브라우저가 아는 값 전부. 지금 값이 목록에 없으면 앞에 붙임(UTC 등) */
function withCurrent(values: string[], current: string) {
  return values.includes(current) ? values : [current, ...values]
}

/** 언어·단위·통화·시간대. 저장하면 I18nProvider 가 새 값으로 화면 전체를 다시 그림 */
export function RegionForm({ user }: { user: UserResponse }) {
  const { replaceUser } = useAuth()
  const queryClient = useQueryClient()
  const { t, locale } = useI18n()

  const [language, setLanguage] = useState(user.language)
  const [unitSystem, setUnitSystem] = useState(user.unitSystem)
  const [currency, setCurrency] = useState(user.currency)
  const [timeZone, setTimeZone] = useState(user.timeZone)
  // 문구 대신 표시 여부·원인. 언어를 바꾼 저장이면 저장 직후의 새 언어로 보여야 함
  const [noChanges, setNoChanges] = useState(false)
  const [saved, setSaved] = useState(false)
  const [failure, setFailure] = useState<{ caught: unknown } | null>(null)
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
    setNoChanges(false)
    setSaved(false)
    setFailure(null)

    // 바뀐 필드만
    const request: UpdateProfileRequest = {}
    if (language !== user.language) request.language = language
    if (unitSystem !== user.unitSystem) request.unitSystem = unitSystem
    if (currency !== user.currency) request.currency = currency
    if (timeZone !== user.timeZone) request.timeZone = timeZone

    if (Object.keys(request).length === 0) {
      setNoChanges(true)
      return
    }

    setPending(true)
    try {
      replaceUser(await updateProfile(request))
      // 통화·시간대가 바뀌면 연비 요약의 합계·지남 판정이 달라짐. 옛 캐시 버림
      queryClient.removeQueries({ queryKey: queryKeys.allVehicles() })
      setSaved(true)
    } catch (caught) {
      setFailure({ caught })
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

          {noChanges && <NoticeText message={t.common.noChanges} />}
          {saved && <NoticeText message={t.common.saved} />}
          {failure !== null && <ErrorText message={errorMessage(failure.caught, t, t.profile.region.failed)} />}

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
