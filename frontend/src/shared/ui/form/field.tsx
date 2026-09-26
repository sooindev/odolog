import type { ReactNode } from 'react'
import { cn } from 'cn'

import { Label } from '@/shared/ui/base/label'

/**
 * 라벨 + 입력 + 도움말 한 벌
 * htmlFor 필수. 라벨 클릭 포커스·스크린리더 연결
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
      {/* cn() 밖이라 토큰 사용 가능 */}
      {hint !== undefined && <p className="text-caption text-muted-foreground">{hint}</p>}
    </div>
  )
}
