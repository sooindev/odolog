import { Monitor, Moon, Sun } from 'lucide-react'
import { cn } from 'cn'

import { useTheme } from '@/shared/theme/context/ThemeContext'
import type { Theme } from '@/shared/theme/context/ThemeContext'

/*
 * macOS 시스템 설정의 '외관' 과 같은 형태다. 해/모니터/달 세 칸이 나란히 있고
 * 선택된 칸 아래로 블록 하나가 미끄러진다.
 *
 * 해/달 아이콘 하나만 두고 누를 때마다 뒤집는 방식을 쓰지 않은 이유:
 * 'system'(OS를 따름)이라는 선택지를 표현할 자리가 없어진다. 그리고 아이콘 하나짜리
 * 토글은 "지금이 다크라는 뜻인지, 누르면 다크가 된다는 뜻인지"가 늘 헷갈린다.
 * 세 칸이 다 보이면 현재 상태와 가능한 선택지를 동시에 알 수 있다.
 */
const OPTIONS = [
  { value: 'light', label: '라이트 모드', Icon: Sun },
  { value: 'system', label: '시스템 설정 따름', Icon: Monitor },
  { value: 'dark', label: '다크 모드', Icon: Moon },
] as const satisfies readonly { value: Theme; label: string; Icon: typeof Sun }[]

/** 칸 하나의 크기(px). 블록을 몇 px 옮길지 계산하는 데 그대로 쓰인다. */
const CELL = 28

export function ThemeToggle({ className }: { className?: string }) {
  const { theme, setTheme } = useTheme()

  const index = OPTIONS.findIndex((option) => option.value === theme)

  return (
    // role="group" + aria-label: 스크린리더가 버튼 셋을 "화면 모드"라는 한 덩어리로 읽는다.
    <div
      role="group"
      aria-label="화면 모드"
      className={cn(
        'relative flex items-center border border-border bg-sunken p-[3px]',
        className,
      )}
    >
      {/*
        움직이는 블록. 버튼마다 배경을 켜고 끄는 대신 하나짜리 요소를 옮긴다.
        배경을 켜고 끄면 "꺼짐 → 켜짐" 두 애니메이션이 따로 돌아 툭 끊기는데,
        하나를 옮기면 선택이 한 덩어리로 이어져 움직인다.
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
          // aria-pressed: 이 버튼이 지금 눌린 상태인지 알려준다. 셋 중 하나만 true다.
          aria-pressed={theme === value}
          aria-label={label}
          title={label}
          // event.currentTarget = 방금 누른 이 버튼. 그 좌표에서 테마가 원형으로 번진다.
          onClick={(event) => setTheme(value, event.currentTarget)}
          className={cn(
            // z-10: 위의 블록보다 위에 있어야 클릭이 버튼에 닿는다.
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
