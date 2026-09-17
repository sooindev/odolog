import { Monitor, Moon, Sun } from 'lucide-react'
import { cn } from 'cn'

import { useTheme } from '@/shared/theme/context/ThemeContext'
import type { Theme } from '@/shared/theme/context/ThemeContext'

/*
 * macOS 시스템 설정의 '외관' 과 같은 형태. 해/모니터/달 세 칸에 블록 하나가 미끄러짐
 * 아이콘 하나짜리 토글을 안 쓰는 이유 — system 을 표현할 자리가 없고,
 * "지금이 다크"인지 "누르면 다크"인지가 늘 헷갈림
 */
const OPTIONS = [
  { value: 'light', label: '라이트 모드', Icon: Sun },
  { value: 'system', label: '시스템 설정 따름', Icon: Monitor },
  { value: 'dark', label: '다크 모드', Icon: Moon },
] as const satisfies readonly { value: Theme; label: string; Icon: typeof Sun }[]

/** 칸 하나의 크기(px). 블록 이동 거리 계산의 기준 */
const CELL = 28

export function ThemeToggle({ className }: { className?: string }) {
  const { theme, setTheme } = useTheme()

  const index = OPTIONS.findIndex((option) => option.value === theme)

  return (
    // 버튼 셋을 "화면 모드" 한 덩어리로 읽히게
    <div
      role="group"
      aria-label="화면 모드"
      className={cn(
        'relative flex items-center border border-border bg-sunken p-[3px]',
        className,
      )}
    >
      {/*
        움직이는 블록. 버튼마다 배경을 켜고 끄는 대신 요소 하나를 이동
        켜고 끄면 두 애니메이션이 따로 돌아 끊겨 보임
      */}
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
          // 누른 버튼 좌표에서 원형으로 번짐
          onClick={(event) => setTheme(value, event.currentTarget)}
          className={cn(
            // z-10 — 블록보다 위에 있어야 클릭이 버튼에 닿음
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
