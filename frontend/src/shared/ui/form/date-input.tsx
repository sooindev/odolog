import { useCallback, useEffect, useRef, useState } from 'react'

import { cn } from 'cn'
import { controlClassName } from '@/shared/ui/form/control'
import { useI18n } from '@/shared/i18n/I18nContext'
import { todayString } from '@/shared/lib/format'
import {
  join,
  lastSelectableDay,
  lastSelectableMonth,
  parse,
  partOrder,
  todayParts,
} from '@/shared/ui/form/date-parts'
import type { DatePart } from '@/shared/ui/form/date-parts'

/** 한 칸 높이(px). 스크롤 위치 ↔ 인덱스 변환 기준 */
const ITEM_HEIGHT = 40
/** 보이는 칸 수. 홀수라 가운데가 선택 자리 */
const VISIBLE = 5
const PAD = ((VISIBLE - 1) / 2) * ITEM_HEIGHT

const YEARS_BACK = 20

/**
 * 드럼 휠 날짜 선택기. 터치 전용
 * 드래그는 세로 스크롤, 칸 맞춤은 scroll-snap
 */
function DateWheel({
  id,
  value,
  onChange,
}: {
  id: string
  value: string
  onChange: (value: string) => void
}) {
  const { t, f, locale, timeZone } = useI18n()
  const [open, setOpen] = useState(false)
  const wheelRef = useRef<HTMLDivElement>(null)
  // 오늘·절단 기준은 계정 시간대. 서버의 미래 판정과 같은 선
  const { year, month, day } = parse(value, timeZone)

  // 펼친 휠을 화면 안으로. block: 'nearest' 로 필요할 때만 이동
  useEffect(() => {
    if (!open) return
    wheelRef.current?.scrollIntoView({ block: 'nearest', behavior: 'smooth' })
  }, [open])

  // 칸 목록도 오늘까지만. 데스크톱의 max 와 같은 선
  const thisYear = todayParts(timeZone).year
  // 기본 20년치, 현재 값이 더 오래됐으면 그 해까지 확장
  const firstYear = Math.min(thisYear - YEARS_BACK, year)
  const columns: Record<DatePart, { label: string; values: number[]; value: number; pick: (next: number) => string }> = {
    year: {
      label: t.dateWheel.year,
      values: Array.from({ length: thisYear - firstYear + 1 }, (_, i) => firstYear + i),
      value: year,
      pick: (next) => join(next, month, day, timeZone),
    },
    month: {
      label: t.dateWheel.month,
      values: Array.from({ length: lastSelectableMonth(year, timeZone) }, (_, i) => i + 1),
      value: month,
      pick: (next) => join(year, next, day, timeZone),
    },
    day: {
      label: t.dateWheel.day,
      values: Array.from({ length: lastSelectableDay(year, month, timeZone) }, (_, i) => i + 1),
      value: day,
      pick: (next) => join(year, month, next, timeZone),
    },
  }

  return (
    <div className="flex flex-col">
      {/* 닫힌 상태는 입력창 모양의 button. Field 의 htmlFor 연결 유지 */}
      <button
        id={id}
        type="button"
        aria-expanded={open}
        className={cn(controlClassName, 'flex items-center justify-between text-left')}
        onClick={() => setOpen((current) => !current)}
      >
        <span className="tabular-nums">{f.date(value)}</span>
        <span className="text-caption text-muted-foreground">
          {open ? t.dateWheel.done : t.dateWheel.change}
        </span>
      </button>

      {open && (
        // 펼침 연출. 닫을 때는 없음
        <div className="form-open">
          <div>
            <div ref={wheelRef} className="relative mt-2 flex border border-border bg-fill">
              {/* 가운데 선택 띠. pointer-events 없음 */}
              <div
                aria-hidden="true"
                className="pointer-events-none absolute inset-x-0 top-1/2 -translate-y-1/2 border-y border-border-strong bg-wash"
                style={{ height: ITEM_HEIGHT }}
              />

              {/* 칸 순서는 로케일. 한국 년·월·일, 미국 월·일·년 */}
              {partOrder(locale).map((part) => (
                <WheelColumn
                  key={part}
                  // 접미사가 없는 언어(영어)는 칸 이름을 스크린리더용으로
                  label={columns[part].label}
                  ariaLabel={columns[part].label || part}
                  values={columns[part].values}
                  value={columns[part].value}
                  onChange={(next) => onChange(columns[part].pick(next))}
                />
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

function WheelColumn({
  label,
  ariaLabel,
  values,
  value,
  onChange,
}: {
  label: string
  ariaLabel: string
  values: number[]
  value: number
  onChange: (value: number) => void
}) {
  const ref = useRef<HTMLDivElement>(null)
  const index = values.indexOf(value)

  const commit = useCallback(() => {
    const el = ref.current
    if (el === null) return

    const next = values[Math.round(el.scrollTop / ITEM_HEIGHT)]
    if (next !== undefined && next !== value) {
      onChange(next)
    }
  }, [values, value, onChange])

  // 값 변경 시 해당 위치로 스크롤. 일수 보정 때만 실제 이동
  useEffect(() => {
    const el = ref.current
    if (el === null || index < 0) return

    const top = index * ITEM_HEIGHT
    if (Math.abs(el.scrollTop - top) > 1) {
      el.scrollTop = top
    }
  }, [index])

  useEffect(() => {
    const el = ref.current
    if (el === null) return

    // scrollend(없으면 마지막 scroll 후 대기) 시점에 확정. 도중 커밋 방지
    if ('onscrollend' in window) {
      el.addEventListener('scrollend', commit)
      return () => el.removeEventListener('scrollend', commit)
    }

    let timer = 0
    const onScroll = () => {
      window.clearTimeout(timer)
      timer = window.setTimeout(commit, 120)
    }
    el.addEventListener('scroll', onScroll)
    return () => {
      window.clearTimeout(timer)
      el.removeEventListener('scroll', onScroll)
    }
  }, [commit])

  function handleKeyDown(event: React.KeyboardEvent) {
    // 키보드 조작 지원
    const step = event.key === 'ArrowDown' ? 1 : event.key === 'ArrowUp' ? -1 : 0
    if (step === 0) return

    event.preventDefault()
    const next = values[index + step]
    if (next !== undefined) onChange(next)
  }

  return (
    <div
      ref={ref}
      role="listbox"
      aria-label={ariaLabel}
      tabIndex={0}
      onKeyDown={handleKeyDown}
      // overscroll-contain 필수. 바깥 페이지로의 스크롤 연쇄 차단
      className="flex-1 snap-y snap-mandatory overflow-y-scroll overscroll-contain outline-none [scrollbar-width:none] focus-visible:ring-[3px] focus-visible:ring-ring/15 [&::-webkit-scrollbar]:hidden"
      style={{ height: VISIBLE * ITEM_HEIGHT, paddingBlock: PAD }}
    >
      {values.map((entry) => (
        <div
          key={entry}
          role="option"
          aria-selected={entry === value}
          className={cn(
            'flex snap-center items-center justify-center text-base tabular-nums transition-colors duration-200 ease-apple',
            entry === value ? 'text-strong' : 'text-faint',
          )}
          style={{ height: ITEM_HEIGHT }}
        >
          {entry}
          {label !== '' && <span className="ml-0.5 text-unit text-muted-foreground">{label}</span>}
        </div>
      ))}
    </div>
  )
}

/**
 * 날짜 입력. 터치면 드럼 휠, 아니면 네이티브 date
 * 판단은 matchMedia('(pointer: coarse)')
 */
export function DateInput({
  id,
  value,
  onChange,
  required,
}: {
  id: string
  value: string
  onChange: (value: string) => void
  /** 네이티브 입력 전용. 휠은 빈 값 불가 */
  required?: boolean
}) {
  const { timeZone } = useI18n()
  // 초기값은 useState 초기화 함수에서. 첫 프레임 컴포넌트 교체 방지
  const [coarse, setCoarse] = useState(() => window.matchMedia('(pointer: coarse)').matches)

  useEffect(() => {
    // 포인터 종류 변경 추적
    const query = window.matchMedia('(pointer: coarse)')
    const onChangeQuery = (event: MediaQueryListEvent) => setCoarse(event.matches)

    query.addEventListener('change', onChangeQuery)
    return () => query.removeEventListener('change', onChangeQuery)
  }, [])

  if (!coarse) {
    return (
      <input
        id={id}
        type="date"
        required={required}
        // 오늘까지만(계정 시간대). 휠·서버와 같은 선
        max={todayString(timeZone)}
        className={controlClassName}
        value={value}
        onChange={(event) => onChange(event.target.value)}
      />
    )
  }

  return <DateWheel id={id} value={value} onChange={onChange} />
}
