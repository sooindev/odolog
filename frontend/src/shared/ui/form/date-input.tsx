import { useCallback, useEffect, useRef, useState } from 'react'

import { cn } from 'cn'
import { controlClassName } from '@/shared/ui/form/control'
import { formatDate } from '@/shared/lib/format/format'

/** 한 칸의 높이(px). 스크롤 위치 ↔ 선택 인덱스 변환이 전부 이 값에 걸려 있다. */
const ITEM_HEIGHT = 40
/** 보이는 칸 수. 홀수라야 가운데 한 칸이 정확히 선택 자리가 된다. */
const VISIBLE = 5
const PAD = ((VISIBLE - 1) / 2) * ITEM_HEIGHT

const YEARS_BACK = 20

function daysInMonth(year: number, month: number) {
  // month 는 1부터. 다음 달 0일 = 이번 달 마지막 날. 윤년도 알아서 맞는다.
  return new Date(year, month, 0).getDate()
}

function todayParts() {
  const now = new Date()
  return { year: now.getFullYear(), month: now.getMonth() + 1, day: now.getDate() }
}

/**
 * 'YYYY-MM-DD' 를 쪼갠다. 빈 값이나 깨진 값이면 오늘로 돌린다.
 *
 * <p>네이티브 date 입력은 사용자가 지우면 빈 문자열을 준다. 그 상태로 화면을 돌리거나
 * 태블릿을 키보드에서 빼면 휠이 그 값을 받는데, 그대로 두면 NaN 이 화면에 찍힌다.
 */
function parse(value: string) {
  const [year, month, day] = value.split('-').map(Number)
  if (!Number.isInteger(year) || !Number.isInteger(month) || !Number.isInteger(day)) {
    return todayParts()
  }
  return { year, month, day }
}

function join(year: number, month: number, day: number) {
  // 일수가 줄어드는 달로 옮기면 잘라 낸다. 1/31 에서 2월로 가면 2/28 이다.
  const clamped = Math.min(day, daysInMonth(year, month))
  return `${year}-${String(month).padStart(2, '0')}-${String(clamped).padStart(2, '0')}`
}

/**
 * 손가락으로 굴리는 날짜 선택기.
 *
 * <p><b>드래그를 직접 만들지 않는다.</b> 세로 스크롤이 곧 드래그이고, scroll-snap 이 칸 맞춤을
 * 해 준다. pointer 이벤트로 구현하면 관성·경계 바운스·스크린리더 대응을 전부 다시 만들어야
 * 하는데, 브라우저가 이미 다 갖고 있다.
 *
 * <p>터치 기기에서만 쓴다. 마우스·키보드에서는 네이티브 date 입력이 더 빠르다 —
 * 날짜를 직접 타이핑할 수 있기 때문이다. 판단은 DateInput 이 한다.
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

  /*
   * 펼쳐진 휠이 화면 밖이면 아무 일도 안 일어난 것처럼 보인다. 정비 폼은 필드가 다섯 개라
   * 맨 아래 날짜 칸에서 누르면 실제로 그렇게 된다.
   * block: 'nearest' — 이미 보이면 가만두고, 잘렸을 때만 그만큼만 따라간다.
   */
  useEffect(() => {
    if (!open) return
    wheelRef.current?.scrollIntoView({ block: 'nearest', behavior: 'smooth' })
  }, [open])

  const thisYear = new Date().getFullYear()
  const years = Array.from({ length: YEARS_BACK + 1 }, (_, i) => thisYear - YEARS_BACK + i)
  const months = Array.from({ length: 12 }, (_, i) => i + 1)
  const days = Array.from({ length: daysInMonth(year, month) }, (_, i) => i + 1)

  return (
    <div className="flex flex-col">
      {/* 닫혀 있을 때는 입력창처럼 보인다. button 도 label 이 가리킬 수 있는 요소라
          Field 의 htmlFor 가 그대로 동작한다. */}
      <button
        id={id}
        type="button"
        aria-expanded={open}
        className={cn(controlClassName, 'flex items-center justify-between text-left')}
        onClick={() => setOpen((current) => !current)}
      >
        <span className="tabular-nums">{formatDate(value)}</span>
        <span className="text-[0.8125rem] text-muted-foreground">{open ? '완료' : '변경'}</span>
      </button>

      {open && (
        // 정비 폼이 펼쳐질 때와 같은 연출. 닫을 때는 연출하지 않는다.
        <div className="form-open">
          <div>
            <div ref={wheelRef} className="relative mt-2 flex border border-border bg-fill">
              {/* 가운데 선택 띠. 칸들 뒤에 깔고 pointer-events 를 꺼서 스크롤을 막지 않는다. */}
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

  /*
   * 값이 바뀌면 그 자리로 맞춘다. 사용자가 굴려서 바뀐 경우에는 이미 그 자리라 아무 일도
   * 일어나지 않고, **잘려 나간 경우에만** 실제로 움직인다(31일에서 2월로 가면 28일로).
   * 그래서 "내가 굴린 것인지" 를 따로 기억할 필요가 없다.
   */
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

    // scrollend 는 "관성까지 멈춘 순간"을 알려 준다. 없는 브라우저에서는 마지막 scroll 에서
    // 조금 기다린다 — 굴리는 도중에 값을 확정하면 지나가는 숫자마다 커밋된다.
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
    // 터치에는 없지만 키보드에는 있어야 한다. 스크롤만으로 조작되면 키보드 사용자는 못 쓴다.
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
      /*
       * overscroll-contain 이 핵심이다. 없으면 칸을 끝까지 굴렸을 때 남은 관성이 바깥
       * 페이지로 넘어가(scroll chaining) 화면이 통째로 스크롤된다 — 폼 중간에서 날짜를
       * 고르다 화면이 저 아래로 날아간다. 네이티브 스크롤을 쓰면 관성·스냅은 공짜로 얻지만
       * **연쇄만은 명시적으로 꺼야 한다.**
       */
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
 * 날짜 입력. 터치 기기에서는 드럼 휠, 그 외에는 네이티브 date 입력이다.
 *
 * <p>둘을 나눈 이유: 마우스·키보드에서는 날짜를 **타이핑**하는 게 제일 빠르고, 드럼을 마우스
 * 휠로 굴리는 건 그보다 느리다. 반대로 폰에서는 OS 캘린더가 뜨는데 달을 넘겨 가며 찾아야 한다.
 *
 * <p>프로젝트가 이미 `@media (pointer: coarse)` 로 터치를 가르고 있어(디자인 11-1) 같은 기준을 쓴다.
 * CSS 로는 컴포넌트를 갈아 끼울 수 없어 matchMedia 로 본다.
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
  /**
   * 네이티브 date 입력에만 붙는다. 사용자가 값을 지울 수 있는 건 그쪽뿐이라
   * 막을 것도 그쪽뿐이다 — 휠은 join() 이 언제나 완전한 날짜를 만들어 빈 값이 될 수 없다.
   */
  required?: boolean
}) {
  // 초기값을 effect 에서 setState 로 채우면 첫 프레임에 네이티브 입력이 보였다가 휠로
  // 바뀌면서 렌더가 한 번 더 돈다(oxlint react(set-state-in-effect)).
  // 지금 값은 렌더 시점에 바로 읽을 수 있으므로 초기화 함수에서 읽는다.
  const [coarse, setCoarse] = useState(() => window.matchMedia('(pointer: coarse)').matches)

  useEffect(() => {
    // effect 는 "바깥 세계와 동기화"만 한다 — 태블릿을 키보드에 꽂는 등 도중에 바뀌는 경우.
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
        className={controlClassName}
        value={value}
        onChange={(event) => onChange(event.target.value)}
      />
    )
  }

  return <DateWheel id={id} value={value} onChange={onChange} />
}
