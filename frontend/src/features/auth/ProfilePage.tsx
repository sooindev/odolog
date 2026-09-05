import { useState } from 'react'
import type { FormEvent } from 'react'

import { useAuth } from '@/features/auth/AuthContext'
import { Button } from '@/shared/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card'
import { Input } from '@/shared/ui/input'
import { Label } from '@/shared/ui/label'
import { ApiError, api } from '@/shared/api/client'
import type { UpdateProfileRequest, UserResponse } from '@/shared/api/types'

export function ProfilePage() {
  const { user } = useAuth()

  // 여기서 null을 걸러내고, 아래 폼에는 확정된 user를 props로 넘긴다.
  // 이렇게 나누면 폼 안에서 user가 null인지 다시 따질 필요가 없다.
  if (user === null) {
    return null
  }

  return <ProfileForm user={user} />
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

    // 백엔드가 "보낸 필드만 변경"이므로 바뀐 것만 담는다. 전부 보내면 의도치 않은 덮어쓰기가 생긴다.
    const request: UpdateProfileRequest = {}
    if (nickname !== user.nickname) request.nickname = nickname
    if (phone !== (user.phone ?? '')) request.phone = phone

    if (Object.keys(request).length === 0) {
      setMessage('변경된 내용이 없습니다.')
      return
    }

    setPending(true)
    try {
      replaceUser(await api.patch<UserResponse>('/api/users/me', request))
      setMessage('저장했습니다.')
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : '저장에 실패했습니다.')
    } finally {
      setPending(false)
    }
  }

  return (
    <Card className="mx-auto max-w-sm">
      <CardHeader>
        <CardTitle>내 정보</CardTitle>
      </CardHeader>
      <CardContent>
        <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="email">이메일</Label>
            {/* 이메일은 수정 API가 없다. 보여주기만 한다. */}
            <Input id="email" value={user.email} disabled />
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="nickname">닉네임</Label>
            <Input
              id="nickname"
              required
              maxLength={30}
              value={nickname}
              onChange={(event) => setNickname(event.target.value)}
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="phone">전화번호</Label>
            <Input
              id="phone"
              maxLength={20}
              value={phone}
              onChange={(event) => setPhone(event.target.value)}
            />
          </div>

          {message !== null && <p className="text-muted-foreground text-sm">{message}</p>}
          {error !== null && <p className="text-destructive text-sm">{error}</p>}

          <Button type="submit" disabled={pending}>
            {pending ? '저장 중…' : '저장'}
          </Button>
        </form>
      </CardContent>
    </Card>
  )
}
