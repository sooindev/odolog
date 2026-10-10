import type { ReactNode } from 'react'
import { cn } from '@/shared/ui/cn'

import { Label } from '@/shared/ui/base/label'
import { FieldContext } from '@/shared/ui/form/field-context'

/**
 * 라벨 + 입력 + 도움말 한 벌
 * htmlFor 필수. 라벨 클릭 포커스·스크린리더 연결, 도움말은 같은 id 의 칸에 aria-describedby
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
  const hintId = hint === undefined ? undefined : `${htmlFor}-hint`

  return (
    <div className={cn('flex flex-col gap-2', className)}>
      <Label htmlFor={htmlFor}>{label}</Label>
      <FieldContext value={{ htmlFor, hintId }}>{children}</FieldContext>
      {hint !== undefined && (
        <p id={hintId} className="text-caption text-muted-foreground">
          {hint}
        </p>
      )}
    </div>
  )
}
