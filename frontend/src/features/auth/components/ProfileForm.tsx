import { useState } from 'react'
import type { FormEvent } from 'react'

import { updateProfile } from '@/features/auth/api/endpoints'
import type { UserResponse } from '@/features/auth/api/types'
import { useAuth } from '@/features/auth/context/AuthContext'
import { Button } from '@/shared/ui/base/button'
import { Card, CardContent } from '@/shared/ui/base/card'
import { Input } from '@/shared/ui/base/input'
import { Field } from '@/shared/ui/form/field'
import { FormActions } from '@/shared/ui/layout/page'
import { ErrorText, NoticeText } from '@/shared/ui/state'
import { useI18n } from '@/shared/i18n/I18nContext'
import { errorMessage } from '@/shared/i18n/errorMessage'

export function ProfileForm({ user }: { user: UserResponse }) {
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
