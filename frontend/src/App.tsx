import { useState } from 'react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { ApiError, api } from '@/lib/api'
import type { LoginRequest, SignUpRequest, UserResponse } from '@/types/api'

/**
 * Phase 2 연동 확인용 임시 화면.
 * 세션 쿠키가 백엔드와 실제로 오가는지만 검증한다. Phase 3에서 라우팅 화면으로 교체 예정.
 */
function App() {
  const [email, setEmail] = useState('front@odolog.com')
  const [password, setPassword] = useState('password123')
  const [result, setResult] = useState('아직 요청하지 않음')
  const [pending, setPending] = useState(false)

  // 어떤 요청이든 로딩/에러 처리가 같아서 한 곳으로 모았다.
  async function run(label: string, action: () => Promise<unknown>) {
    setPending(true)
    try {
      const data = await action()
      setResult(`${label} 성공\n${JSON.stringify(data ?? null, null, 2)}`)
    } catch (error) {
      const message =
        error instanceof ApiError ? `[${error.status}] ${error.message}` : String(error)
      setResult(`${label} 실패\n${message}`)
    } finally {
      setPending(false)
    }
  }

  return (
    <div className="min-h-screen bg-muted/40 p-8">
      <div className="mx-auto flex max-w-xl flex-col gap-4">
        <div>
          <h1 className="text-2xl font-bold">오도로그</h1>
          <p className="text-muted-foreground text-sm">백엔드 연동 확인 (임시 화면)</p>
        </div>

        <Card>
          <CardHeader>
            <CardTitle>계정</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-3">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="email">이메일</Label>
              <Input id="email" value={email} onChange={(e) => setEmail(e.target.value)} />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="password">비밀번호</Label>
              <Input
                id="password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </div>
          </CardContent>
        </Card>

        <div className="flex flex-wrap gap-2">
          <Button
            disabled={pending}
            onClick={() =>
              run('회원가입', () =>
                api.post<UserResponse>('/api/users', {
                  email,
                  password,
                  nickname: '프론트',
                  phone: '010-1111-2222',
                } satisfies SignUpRequest),
              )
            }
          >
            회원가입
          </Button>
          <Button
            disabled={pending}
            onClick={() =>
              run('로그인', () =>
                api.post<UserResponse>('/api/users/login', {
                  email,
                  password,
                } satisfies LoginRequest),
              )
            }
          >
            로그인
          </Button>
          <Button
            variant="outline"
            disabled={pending}
            onClick={() => run('내 정보', () => api.get<UserResponse>('/api/users/me'))}
          >
            내 정보 (쿠키 확인)
          </Button>
          <Button
            variant="outline"
            disabled={pending}
            onClick={() => run('로그아웃', () => api.post('/api/users/logout'))}
          >
            로그아웃
          </Button>
        </div>

        <pre className="bg-card overflow-x-auto rounded-lg border p-4 text-sm whitespace-pre-wrap">
          {result}
        </pre>
      </div>
    </div>
  )
}

export default App
