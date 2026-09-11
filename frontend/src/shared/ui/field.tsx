import type { ReactNode } from 'react'
import { cn } from 'cn'

import { Label } from '@/shared/ui/label'

/**
 * 라벨 + 입력 + 도움말 한 벌.
 *
 * 이 세 줄짜리 묶음이 화면 6곳에 15번 넘게 복사돼 있었다(`flex flex-col gap-1.5`).
 * 폼 간격을 조정하려면 15곳을 다 고쳐야 했는데, 간격은 디자인 시스템에서 가장 자주
 * 손대는 값이다. 한 곳으로 모은다.
 *
 * htmlFor 를 필수로 받는 이유: <label for>와 <input id>가 이어져 있어야
 * 라벨을 눌렀을 때 입력창이 포커스되고, 스크린리더가 이름을 읽어 준다.
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
      {hint !== undefined && <p className="text-xs text-muted-foreground">{hint}</p>}
    </div>
  )
}
