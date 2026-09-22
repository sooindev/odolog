import { useCallback, useEffect, useRef, useState } from 'react'

import { cn } from 'cn'
import { controlClassName } from '@/shared/ui/form/control'
import { formatDate, todayString } from '@/shared/lib/format/format'
import { join, lastSelectableDay, lastSelectableMonth, parse } from '@/shared/ui/form/date-parts'

/** 한 칸 높이(px). 스크롤 위치 ↔ 선택 인덱스 변환의 기준 */
const ITEM_HEIGHT = 40
/** 보이는 칸 수. 홀수라야 가운데가 선택 자리 */
const VISIBLE = 5
const PAD = ((VISIBLE - 1) / 2) * ITEM_HEIGHT

const YEARS_BACK = 20

/**
 * 드럼 휠 날짜 선택기. 터치 전용
 * 드래그를 직접 구현하지 않음 — 세로 스크롤이 곧 드래그, 칸 맞춤은 scroll-snap
 * 관성·바운스·스크린리더를 브라우저가 이미 갖고 있음
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
  const [open, setOpen] = useState(false)
  const wheelRef = useRef<HTMLDivElement>(null)
  const { year, month, day } = parse(value)

  // 펼친 휠이 화면 밖이면 아무 일도 안 일어난 것처럼 보임
  // block: 'nearest' — 이미 보이면 그대로, 잘렸을 때만 그만큼
  useEffect(() => {
    if (!open) return
    wheelRef.current?.scrollIntoView({ block: 'nearest', behavior: 'smooth' })
  }, [open])

  /*
   * 칸 목록도 오늘에서 끊는다. 년만 막고 월·일을 열어 두면 올해 남은 달이 그대로 선택되고,
   * 저장할 때야 서버가 400 을 준다 — 데스크톱의 <input type="date"> 는 max 로 막는 자리다.
   * 고를 수 없는 것을 아예 안 보여주는 편이 막아 놓고 나중에 혼내는 것보다 낫다
   */
  const thisYear = new Date().getFullYear()
  /*
   * 기본은 20년치지만, 지금 값이 그보다 오래됐으면 그 해까지 늘린다.
   * 고정 범위로 두면 값이 목록에 없어 indexOf 가 -1 이 되고, 년 칸이 자리를 못 잡는다.
   * 서버는 @PastOrPresent 라 얼마나 오래된 날짜든 받는다 — 화면만 못 열면 안 된다
   */
  const firstYear = Math.min(thisYear - YEARS_BACK, year)
  const years = Array.from({ length: thisYear - firstYear + 1 }, (_, i) => firstYear + i)
  const months = Array.from({ length: lastSelectableMonth(year) }, (_, i) => i + 1)
  const days = Array.from({ length: lastSelectableDay(year, month) }, (_, i) => i + 1)

  return (
    <div className="flex flex-col">
      {/* 닫혀 있을 때는 입력창처럼. button 도 label 이 가리킬 수 있어 Field 의 htmlFor 가 그대로 동작 */}
      <button
        id={id}
        type="button"
        aria-expanded={open}
        className={cn(controlClassName, 'flex items-center justify-between text-left')}
        onClick={() => setOpen((current) => !current)}
      >
        <span className="tabular-nums">{formatDate(value)}</span>
        <span className="text-caption text-muted-foreground">{open ? '완료' : '변경'}</span>
      </button>

      {open && (
        // 정비 폼과 같은 펼침 연출. 닫을 때는 연출 없음
        <div className="form-open">
          <div>
            <div ref={wheelRef} className="relative mt-2 flex border border-border bg-fill">
              {/* 가운데 선택 띠. 칸들 뒤에 깔고 pointer-events 를 꺼 스크롤을 막지 않음 */}
              <div
                aria-hidden="true"
                className="pointer-events-none absolute inset-x-0 top-1/2 -translate-y-1/2 border-y border-border-strong bg-wash"
                style={{ height: ITEM_HEIGHT }}
              />

              <WheelColumn
                label="년"
                values={years}
                value={year}
                onChange={(next) => onChange(join(next, month, day))}
              />
              <WheelColumn
                label="월"
                values={months}
                value={month}
                onChange={(next) => onChange(join(year, next, day))}
              />
              <WheelColumn
                label="일"
                values={days}
                value={day}
                onChange={(next) => onChange(join(year, month, next))}
              />
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

function WheelColumn({
  label,
  values,
  value,
  onChange,
}: {
  label: string
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

  // 값이 바뀌면 그 자리로 스크롤. 굴려서 바뀐 경우엔 이미 그 자리라 무동작
  // 실제로 움직이는 건 일수가 잘린 경우뿐이라 "내가 굴렸는지"를 기억할 필요가 없음
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

    // scrollend = 관성까지 멈춘 순간. 없으면 마지막 scroll 에서 대기
    // 굴리는 도중에 확정하면 지나가는 숫자마다 커밋됨
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
    // 스크롤로만 조작되면 키보드 사용자는 못 씀
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
      aria-label={label}
      tabIndex={0}
      onKeyDown={handleKeyDown}
      // overscroll-contain 필수. 없으면 남은 관성이 바깥 페이지로 넘어가 화면이 통째로 스크롤됨
      // 관성·스냅은 네이티브로 공짜지만 연쇄만은 명시적으로 꺼야 함
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
          <span className="ml-0.5 text-[0.6875rem] text-muted-foreground">{label}</span>
        </div>
      ))}
    </div>
  )
}

/**
 * 날짜 입력. 터치면 드럼 휠, 아니면 네이티브 date
 * 데스크톱은 타이핑이 제일 빠르고, 폰은 OS 캘린더가 달을 넘겨 가며 찾아야 함
 * 기준은 pointer: coarse — CSS 로는 컴포넌트를 갈아 끼울 수 없어 matchMedia
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
  /** 네이티브 경로 전용. 휠은 join() 이 언제나 완전한 날짜를 만들어 빈 값이 불가능 */
  required?: boolean
}) {
  // effect 에서 setState 로 채우면 첫 프레임에 네이티브가 보였다가 휠로 바뀜
  // 렌더 시점에 이미 읽을 수 있는 값이라 초기화 함수에서
  const [coarse, setCoarse] = useState(() => window.matchMedia('(pointer: coarse)').matches)

  useEffect(() => {
    // effect 는 바깥 세계와 동기화만 — 태블릿을 키보드에 꽂는 등
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
        // 휠은 미래 년도를 아예 안 만든다. 이쪽도 같은 선을 그어야 기기마다 다르게 동작하지 않음
        max={todayString()}
        className={controlClassName}
        value={value}
        onChange={(event) => onChange(event.target.value)}
      />
    )
  }

  return <DateWheel id={id} value={value} onChange={onChange} />
}
