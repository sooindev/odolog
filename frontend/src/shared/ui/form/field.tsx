import type { ReactNode } from 'react'
import { cn } from 'cn'

import { Label } from '@/shared/ui/base/label'

/**
 * 라벨 + 입력 + 도움말 한 벌
 * 화면 6곳에 15번 넘게 복사돼 있던 묶음. 간격은 가장 자주 손대는 값이라 한 곳으로 모음
 * htmlFor 필수 — label for 와 input id 가 이어져야 라벨 클릭 포커스와 스크린리더 읽기가 동작
 */
export function Field({
  label,
  htmlFor,
  hint,
  className,
  children,
}: {
  label: string
  htmlFor: string
  hint?: string
  className?: string
  children: ReactNode
}) {
  return (
    <div className={cn('flex flex-col gap-2', className)}>
      <Label htmlFor={htmlFor}>{label}</Label>
      {children}
      {/* cn() 밖이라 토큰을 쓸 수 있다. 위 div 의 className 은 cn 을 거치므로 거기는 못 쓴다 */}
      {hint !== undefined && <p className="text-caption text-muted-foreground">{hint}</p>}
    </div>
  )
}
