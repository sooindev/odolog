import type { ReactNode } from 'react'
import { ChevronDown } from 'lucide-react'
import { cn } from 'cn'

import { controlClassName } from '@/shared/ui/form/control'

/**
 * 네이티브 select + 같은 톤 화살표. 모바일 OS 선택 UI 사용
 * controlClassName 으로 입력창과 높이·포커스 통일, 펼침 목록은 color-scheme 이 테마 반영
 */
export function NativeSelect({
  id,
  value,
  onChange,
  autoFocus,
  children,
}: {
  id: string
  value: string
  onChange: (value: string) => void
  autoFocus?: boolean
  children: ReactNode
}) {
  return (
    <div className="relative">
      <select
        id={id}
        autoFocus={autoFocus}
        // appearance-none: OS 기본 화살표 제거 후 같은 톤 화살표
        className={cn(controlClassName, 'appearance-none pr-10')}
        value={value}
        onChange={(event) => onChange(event.target.value)}
      >
        {children}
      </select>
      {/* pointer-events-none: select 클릭 가로채기 방지 */}
      <ChevronDown
        className="pointer-events-none absolute top-1/2 right-3.5 size-4 -translate-y-1/2 text-muted-foreground"
        aria-hidden="true"
      />
    </div>
  )
}
