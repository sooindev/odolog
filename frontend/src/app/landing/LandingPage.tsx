import { CalendarClock, Fuel, Gauge, Wrench } from 'lucide-react'
import { Link } from 'react-router'

import { Button } from '@/shared/ui/base/button'
import { GaugeMark } from '@/shared/ui/brand/mark'

/*
 * 로그인 전에 처음 만나는 화면. 비로그인일 때만 렌더되므로 여기엔 로그인 여부를
 * 따지는 코드가 없다. 갈림은 HomePage 가 한다.
 *
 * API 도 상태도 없이 shared/ui 조각을 엮기만 해서 features/ 가 아니라 app/ 에 둔다.
 */
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
  return (
    // 가운데 정렬은 랜딩에서만 쓴다. 앱 화면은 전부 왼쪽 정렬인데, 이 대비 자체가
    // "여기는 아직 앱 바깥"이라는 신호가 된다.
    <section className="flex flex-col items-center gap-8 pt-2 text-center sm:gap-10 sm:pt-20">
      <GaugeMark className="size-11 text-strong" />

      {/* 줄바꿈을 브라우저에 맡기지 않는다(<br/>). 큰 글씨일수록 창 너비에 따라
          끊기는 자리가 달라지는 게 눈에 띈다. 좁은 화면에서는 br 을 숨겨 자연스럽게 흐르게 둔다.
          크기는 화면 제목(text-title)보다 한 단 위다 — 랜딩은 앱 화면이 아니라 표지이고,
          표지의 제목은 본문 지면의 제목보다 커야 표지로 읽힌다. */}
      <h1 className="max-w-4xl text-[clamp(2.5rem,1.5rem+4.4vw,4.5rem)] leading-[1.02] font-semibold tracking-[-0.045em] text-strong">
        마지막 정비가 언제였는지,
        <br className="hidden sm:block" /> 다음은 언제인지.
      </h1>

      <p className="max-w-xl text-lede text-muted-foreground">
        정비 이력과 주유 기록을 남기면, 다음 정비 시점과 연비를 대신 계산합니다.
      </p>

      <div className="mt-4 flex flex-wrap items-center justify-center gap-2">
        <Button size="lg" render={<Link to="/signup" />}>
          시작하기
        </Button>
        <Button size="lg" variant="ghost" render={<Link to="/login" />}>
          로그인
        </Button>
      </div>
    </section>
  )
}

const HIGHLIGHTS = [
  {
    Icon: Wrench,
    title: '정비 이력',
    body: '엔진오일 · 미션오일 · 브레이크 · 타이어 등 15가지 종류. 비용과 메모까지 함께 남깁니다.',
  },
  {
    Icon: CalendarClock,
    title: '다음 정비 시점',
    body: '종류별 권장 주기와 마지막 기록으로 계산합니다. 주행거리와 날짜, 두 기준을 모두 보여줍니다.',
  },
  {
    Icon: Fuel,
    title: '주유와 연비',
    body: '주유할 때마다 주행거리와 넣은 양을 적으면 연비가 나옵니다. 유류비도 함께 쌓입니다.',
  },
  {
    Icon: Gauge,
    title: '주행거리',
    body: '차량마다 따로 기록합니다. 이전보다 작은 값은 애초에 저장되지 않습니다.',
  },
]

function Highlights() {
  return (
    /*
      gap-px + 바깥 배경을 선 색으로: 칸 사이에 1px 틈만 남기고 그 틈으로 뒷배경(선 색)이
      비쳐 보이게 하는 방식이다. 칸마다 border 를 붙이면 맞닿는 자리에서 선이 두 겹이 되어
      1px 이 2px 로 보이는데, 이 방법은 어디서나 정확히 1px 다.
      칸이 4개라 sm 에서 2×2, lg 에서 한 줄이다. sm:grid-cols-3 으로 두면 마지막 칸만 홀로 남는다.
    */
    <section className="reveal grid gap-px overflow-hidden border border-border bg-border sm:grid-cols-2 lg:grid-cols-4">
      {HIGHLIGHTS.map(({ Icon, title, body }) => (
        <div key={title} className="flex flex-col gap-4 bg-background p-8">
          <Icon className="size-5 text-muted-foreground" strokeWidth={1.75} aria-hidden="true" />
          <h2 className="text-[0.9375rem] font-semibold tracking-[-0.01em] text-strong">{title}</h2>
          <p className="text-[0.8125rem] leading-relaxed text-muted-foreground">{body}</p>
        </div>
      ))}
    </section>
  )
}

function Preview() {
  return (
    <section className="reveal flex flex-col gap-10">
      <div className="flex flex-col items-center gap-3 text-center">
        <h2 className="text-[clamp(1.75rem,1.3rem+1.6vw,2.5rem)] leading-[1.1] font-semibold tracking-[-0.035em] text-strong">
          차 한 대의 기록이 한 화면에
        </h2>
        <p className="max-w-md text-[0.9375rem] leading-relaxed text-muted-foreground">
          주행거리와 연비, 다음 정비 시점, 지난 이력을 따로 찾아다닐 필요가 없습니다.
        </p>
      </div>

      {/*
        실제 화면을 캡처해 넣지 않고 같은 토큰으로 다시 그린다. 스크린샷은 디자인이 바뀌는
        순간 옛날 화면이 되어 버리는데, 이건 테마를 바꾸면 이것도 같이 바뀐다.
        카드 안에 카드를 넣어 기기 프레임처럼 보이게 했다.
      */}
      <div className="mx-auto w-full max-w-2xl border border-border bg-card p-3 sm:p-4">
        <div className="flex flex-col gap-8 border border-border bg-sunken p-6 sm:p-8">
          <div className="flex flex-col gap-1.5">
            <p className="text-eyebrow text-muted-foreground uppercase">
              12가 3456
            </p>
            <p className="text-section text-strong">
              현대 아반떼
            </p>
          </div>

          <div className="flex items-baseline gap-2">
            <span className="text-[2.75rem] leading-none font-semibold tracking-[-0.045em] tabular-nums text-strong">
              45,000
            </span>
            <span className="text-sm text-muted-foreground">km</span>
          </div>

          {/* 실제 화면은 이력이 있는 종류만 보여준다. 전에는 여기에 '타이어 · 이력 없음' 이
              있었는데, 일괄 조회로 바꾸면서 그런 줄이 나오지 않게 되어 미리보기가 거짓말을
              하고 있었다. */}
          <ul className="divide-y divide-border border-t border-border">
            {[
              ['평균 연비', '13.4 km/L'],
              ['엔진오일', '50,000km 또는 2027. 1. 15.'],
              ['미션오일', '105,000km 또는 2030. 6. 2.'],
            ].map(([type, next]) => (
              <li key={type} className="flex items-baseline justify-between gap-4 py-3.5">
                <span className="text-[0.9375rem] tracking-[-0.01em] text-strong">{type}</span>
                <span className="text-right text-[0.8125rem] tabular-nums text-muted-foreground">
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
  return (
    <section className="reveal flex flex-col items-center gap-7 border-t border-border pt-20 text-center">
      <h2 className="max-w-lg text-[clamp(1.75rem,1.3rem+1.6vw,2.5rem)] leading-[1.1] font-semibold tracking-[-0.035em] text-strong">
        차 한 대만 있으면 시작할 수 있습니다.
      </h2>

      <Button size="lg" render={<Link to="/signup" />}>
        시작하기
      </Button>

      <p className="text-sm text-muted-foreground">
        이미 계정이 있으신가요?{' '}
        <Link
          to="/login"
          className="text-strong transition-opacity duration-200 ease-apple hover:opacity-70"
        >
          로그인
        </Link>
      </p>
    </section>
  )
}
