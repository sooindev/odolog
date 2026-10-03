import { CalendarClock, Fuel, Gauge, Wrench } from 'lucide-react'
import { Link } from 'react-router'

import { Button } from '@/shared/ui/base/button'
import { GaugeMark } from '@/shared/ui/mark'
import { useI18n } from '@/shared/i18n/I18nContext'

// 소개 화면. 비로그인 전용(갈림은 HomePage)
// API·상태 없는 조립 화면이라 app/ 소속
export function LandingPage() {
  return (
    <div className="flex flex-col gap-20 sm:gap-36">
      <Hero />
      <Highlights />
      <Preview />
      <Closing />
    </div>
  )
}

function Hero() {
  const { t } = useI18n()

  return (
    // 가운데 정렬은 랜딩 전용. 앱 바깥이라는 신호
    <section className="flex flex-col items-center gap-8 pt-2 text-center sm:gap-10 sm:pt-20">
      <GaugeMark className="size-11 text-strong" />

      {/*
        줄바꿈 직접 지정, 좁은 화면에서는 br 숨김
        크기는 화면 제목보다 한 단 위
      */}
      <h1 className="max-w-4xl text-[clamp(2.5rem,1.5rem+4.4vw,4.5rem)] leading-[1.02] font-semibold tracking-[-0.045em] text-strong">
        {t.landing.headline1}
        <br className="hidden sm:block" /> {t.landing.headline2}
      </h1>

      <p className="max-w-xl text-lede text-muted-foreground">
        {t.landing.lede}
      </p>

      <div className="mt-4 flex flex-wrap items-center justify-center gap-2">
        <Button size="lg" render={<Link to="/signup" />}>
          {t.landing.start}
        </Button>
        <Button size="lg" variant="ghost" render={<Link to="/login" />}>
          {t.landing.login}
        </Button>
      </div>
    </section>
  )
}

// 문구는 t.landing.highlights 의 같은 키
const HIGHLIGHTS = [
  { key: 'maintenance', Icon: Wrench },
  { key: 'next', Icon: CalendarClock },
  { key: 'fuel', Icon: Fuel },
  { key: 'odometer', Icon: Gauge },
] as const

function Highlights() {
  const { t } = useI18n()

  return (
    // gap-px 격자로 칸 사이 1px
    // 칸 4개라 sm 2×2, lg 한 줄
    <section className="reveal grid gap-px overflow-hidden border border-border bg-border sm:grid-cols-2 lg:grid-cols-4">
      {HIGHLIGHTS.map(({ key, Icon }) => (
        <div key={key} className="flex flex-col gap-4 bg-background p-8">
          <Icon className="size-5 text-muted-foreground" strokeWidth={1.75} aria-hidden="true" />
          <h2 className="text-body font-semibold tracking-[-0.01em] text-strong">
            {t.landing.highlights[key].title}
          </h2>
          <p className="text-caption leading-relaxed text-muted-foreground">
            {t.landing.highlights[key].body}
          </p>
        </div>
      ))}
    </section>
  )
}

function Preview() {
  const { t, f } = useI18n()

  // 예시 값도 보는 사람의 단위·날짜 표기로
  const rows = [
    [t.landing.previewAverage, f.efficiency(13.4, 1)],
    [t.serviceTypes.ENGINE_OIL, `${f.distance(50_000)}${t.maintenance.next.or}${f.date('2027-01-15')}`],
    [t.serviceTypes.TRANSMISSION_FLUID, `${f.distance(105_000)}${t.maintenance.next.or}${f.date('2030-06-02')}`],
  ]

  return (
    <section className="reveal flex flex-col gap-10">
      <div className="flex flex-col items-center gap-3 text-center">
        <h2 className="text-headline text-strong">{t.landing.previewTitle}</h2>
        <p className="max-w-md text-body leading-relaxed text-muted-foreground">
          {t.landing.previewBody}
        </p>
      </div>

      {/* 캡처 대신 같은 토큰으로 다시 그린 미리보기. 디자인 변경에 자동 반영 */}
      <div className="mx-auto w-full max-w-2xl border border-border bg-card p-3 sm:p-4">
        <div className="flex flex-col gap-8 border border-border bg-sunken p-6 sm:p-8">
          <div className="flex flex-col gap-1.5">
            <p className="text-eyebrow text-muted-foreground uppercase">{t.landing.previewPlate}</p>
            <p className="text-section text-strong">{t.landing.previewModel}</p>
          </div>

          <div className="flex items-baseline gap-2">
            <span className="text-[2.75rem] leading-none font-semibold tracking-[-0.045em] text-strong">
              {f.distanceNumber(45_000)}
            </span>
            <span className="text-caption text-muted-foreground">{f.distanceUnit}</span>
          </div>

          {/* 실제 화면처럼 이력 있는 종류만 표시 */}
          <ul className="divide-y divide-border border-t border-border">
            {rows.map(([type, next]) => (
              <li key={type} className="flex items-baseline justify-between gap-4 py-3.5">
                <span className="text-body text-strong">{type}</span>
                <span className="text-right text-caption tabular-nums text-muted-foreground">
                  {next}
                </span>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </section>
  )
}

function Closing() {
  const { t } = useI18n()

  return (
    <section className="reveal flex flex-col items-center gap-7 border-t border-border pt-20 text-center">
      <h2 className="max-w-lg text-headline text-strong">{t.landing.closing}</h2>

      <Button size="lg" render={<Link to="/signup" />}>
        {t.landing.start}
      </Button>

      <p className="text-caption text-muted-foreground">
        {t.landing.haveAccount}{' '}
        <Link
          to="/login"
          className="text-strong transition-opacity duration-200 ease-apple hover:opacity-70"
        >
          {t.landing.login}
        </Link>
      </p>
    </section>
  )
}
