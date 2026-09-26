import { Monitor, Moon, Sun } from 'lucide-react'
import { cn } from 'cn'

import { useTheme } from '@/shared/theme/context/ThemeContext'
import type { Theme } from '@/shared/theme/context/ThemeContext'

// 해/모니터/달 세 칸 + 미끄러지는 블록
// system 을 표현하려고 세 칸
const OPTIONS = [
  { value: 'light', label: '라이트 모드', Icon: Sun },
  { value: 'system', label: '시스템 설정 따름', Icon: Monitor },
  { value: 'dark', label: '다크 모드', Icon: Moon },
] as const satisfies readonly { value: Theme; label: string; Icon: typeof Sun }[]

/** 칸 크기(px). 블록 이동 거리 기준 */
const CELL = 28

export function ThemeToggle({ className }: { className?: string }) {
  const { theme, setTheme } = useTheme()

  const index = OPTIONS.findIndex((option) => option.value === theme)

  return (
    // 버튼 셋을 한 그룹으로
    <div
      role="group"
      aria-label="화면 모드"
      className={cn(
        'relative flex items-center border border-border bg-sunken p-[3px]',
        className,
      )}
    >
      {/* 움직이는 블록 하나. 버튼별 배경 전환 대신 */}
      <span
        aria-hidden="true"
        className="absolute top-[3px] left-[3px] border border-border bg-fill transition-transform duration-300 ease-apple"
        style={{ width: CELL, height: CELL, transform: `translateX(${index * CELL}px)` }}
      />

      {OPTIONS.map(({ value, label, Icon }) => (
        <button
          key={value}
          type="button"
          // 셋 중 하나만 true
          aria-pressed={theme === value}
          aria-label={label}
          title={label}
          // 누른 버튼 위치에서 원형 전환
          onClick={(event) => setTheme(value, event.currentTarget)}
          className={cn(
            // z-10: 블록 위에서 클릭 수신
            'relative z-10 grid place-items-center transition-colors duration-200 ease-apple focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background',
            theme === value ? 'text-strong' : 'text-muted-foreground hover:text-strong',
          )}
          style={{ width: CELL, height: CELL }}
        >
          <Icon className="size-3.5" strokeWidth={2} />
        </button>
      ))}
    </div>
  )
}
