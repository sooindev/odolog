import { Link, useNavigate } from 'react-router'

import { useAuth } from '@/auth/AuthContext'
import { Button } from '@/components/ui/button'

export function Header() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  async function handleLogout() {
    await logout()
    navigate('/login', { replace: true })
  }

  return (
    <header className="bg-background border-b">
      <div className="mx-auto flex max-w-3xl items-center justify-between px-4 py-3">
        <Link to="/vehicles" className="font-bold">
          오도로그
        </Link>

        {user !== null && (
          <div className="flex items-center gap-3 text-sm">
            <Link to="/me" className="text-muted-foreground hover:underline">
              {user.nickname}
            </Link>
            <Button variant="outline" size="sm" onClick={handleLogout}>
              로그아웃
            </Button>
          </div>
        )}
      </div>
    </header>
  )
}
