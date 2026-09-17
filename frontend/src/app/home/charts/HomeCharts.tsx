import { SERVICE_TYPE_LABELS } from '@/features/maintenance/api/types/types'
import type { MonthlyCost, TypeCost } from '@/app/home/stats/homeStats'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/shared/ui/base/card'
import { formatCompact, formatMonth, formatNumber, formatWon } from '@/shared/lib/format/format'

/*
 * 홈 화면의 두 차트. 라이브러리 없이 HTML·CSS 로만 그린다.
 *
 * 색을 늘리지 않는다. 두 차트 모두 계열이 하나라 색이 구분할 것이 없고, 그래서 범례도 없다.
 * 막대는 얇게, 눈금선은 1px 실선. 점선 눈금은 "예측"이나 "임계선"으로 읽힌다.
 */

/** 축 눈금을 1·2·5 × 10ⁿ 에 맞춘다. 1,873 같은 수로 끝나는 축은 읽을 수 없다. */
function niceMax(value: number) {
  if (value <= 0) return 1

  const magnitude = 10 ** Math.floor(Math.log10(value))
  const normalized = value / magnitude

  const step = normalized <= 1 ? 1 : normalized <= 2 ? 2 : normalized <= 5 ? 5 : 10

  return step * magnitude
}

export function MonthlyCostChart({ monthly }: { monthly: MonthlyCost[] }) {
  const total = monthly.reduce((acc, entry) => acc + entry.cost, 0)
  const peak = Math.max(...monthly.map((entry) => entry.cost))
  const max = niceMax(peak)
  // 값을 직접 적는 막대는 하나뿐. 비용이 같은 달이 둘이면 라벨이 두 개가 되므로
  // 최고값을 가진 첫 칸만 고른다.
  const peakIndex = peak > 0 ? monthly.findIndex((entry) => entry.cost === peak) : -1

  return (
    <Card>
      <CardHeader>
        <CardTitle>지난 12개월 유지비</CardTitle>
        <CardDescription>달마다 들어간 정비비와 유류비의 합입니다.</CardDescription>
      </CardHeader>

      <CardContent className="flex flex-col gap-8">
        {/* 히어로 숫자. tabular-nums 를 쓰지 않는다. 큰 글씨에서는 좁은 글자 주변이
            휑하게 벌어진다. 세로로 자릿수를 맞출 상대가 있을 때만 쓴다. */}
        <div className="flex items-baseline gap-3">
          <span className="text-display text-strong">{formatNumber(total)}</span>
          <span className="text-eyebrow text-muted-foreground uppercase">원</span>
        </div>

        <figure className="flex flex-col gap-2">
          <div className="relative">
            {/* 눈금선. h-0 + items-center 라야 선이 정확히 0/50/100% 에 온다.
                높이가 있으면 글자 높이만큼 밀린다. */}
            <div className="absolute inset-0 flex flex-col justify-between">
              {[max, max / 2, 0].map((tick) => (
                <div key={tick} className="flex h-0 items-center gap-3">
                  <span className="w-8 shrink-0 text-right text-[0.625rem] tabular-nums text-muted-foreground sm:w-10">
                    {formatCompact(tick)}
                  </span>
                  <div className="h-px flex-1 bg-border" />
                </div>
              ))}
            </div>

            {/* z-10: 막대가 눈금선 위로 올라와야 한다. */}
            <div className="relative z-10 ml-10 flex h-40 items-end gap-0.5 sm:ml-[3.25rem] sm:h-44 sm:gap-1">
              {monthly.map((entry, index) => (
                <MonthColumn
                  key={entry.month}
                  entry={entry}
                  index={index}
                  total={monthly.length}
                  max={max}
                  isPeak={index === peakIndex}
                />
              ))}
            </div>
          </div>

          {/* 가로축은 차트 칸 '안'에 둔다. 높이를 고정한 칸 밖으로 밀어내면 축만 잘린다. */}
          <div className="ml-10 flex gap-0.5 sm:ml-[3.25rem] sm:gap-1">
            {monthly.map((entry, index) => (
              // 좁은 화면에서는 칸 하나가 20px 남짓이라 12개를 다 적으면 숫자가 서로 붙는다.
              // 홀수 칸만 남겨 간격을 두 배로 벌린다. 막대는 12개 그대로다.
              <span
                key={entry.month}
                className={`flex-1 text-center text-[0.625rem] tabular-nums text-muted-foreground ${
                  index % 2 === 1 ? 'invisible sm:visible' : ''
                }`}
              >
                {Number(entry.month.slice(5))}
              </span>
            ))}
          </div>
          <figcaption className="ml-10 text-[0.625rem] text-muted-foreground sm:ml-[3.25rem]">월</figcaption>
        </figure>

        {/* 마우스를 올려야만 보이는 값은 키보드·스크린리더 사용자에게 없는 것이나 같다.
            <details> 는 접기/펴기와 키보드 조작을 브라우저가 해 준다. */}
        <details className="group">
          <summary className="w-fit cursor-pointer list-none text-[0.8125rem] text-muted-foreground transition-opacity duration-200 ease-apple hover:opacity-70">
            표로 보기
          </summary>
          {/* 열이 5개라 좁은 화면에서는 넘친다. 표는 가로 스크롤을 허용하는 예외다 —
              글이 담긴 열을 좁히는 것보다 옆으로 미는 편이 읽기 낫다. */}
          {/* overscroll-x-contain: iOS 사파리에서 가로 스크롤이 끝에 닿으면 뒤로가기
              제스처로 넘어갈 수 있다. 표를 밀다가 화면이 바뀌면 안 된다. */}
          <div className="mt-4 overflow-x-auto overscroll-x-contain">
            <table className="w-full min-w-[26rem] text-[0.8125rem]">
              <thead>
                <tr className="border-b border-border text-left text-muted-foreground">
                  <th scope="col" className="pb-2 font-medium">
                    월
                  </th>
                  <th scope="col" className="pb-2 text-right font-medium">
                    건수
                  </th>
                  <th scope="col" className="pb-2 text-right font-medium">
                    정비
                  </th>
                  <th scope="col" className="pb-2 text-right font-medium">
                    주유
                  </th>
                  <th scope="col" className="pb-2 text-right font-medium">
                    합계
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {monthly.map((entry) => (
                  <tr key={entry.month}>
                    <td className="py-2 tabular-nums text-foreground">{formatMonth(entry.month)}</td>
                    <td className="py-2 text-right tabular-nums text-muted-foreground">
                      {entry.count}
                    </td>
                    <td className="py-2 text-right tabular-nums text-muted-foreground">
                      {formatWon(entry.maintenanceCost)}
                    </td>
                    <td className="py-2 text-right tabular-nums text-muted-foreground">
                      {formatWon(entry.fuelCost)}
                    </td>
                    <td className="py-2 text-right tabular-nums text-strong">
                      {formatWon(entry.cost)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </details>
      </CardContent>
    </Card>
  )
}

function MonthColumn({
  entry,
  index,
  total,
  max,
  isPeak,
}: {
  entry: MonthlyCost
  index: number
  total: number
  max: number
  isPeak: boolean
}) {
  const percent = (entry.cost / max) * 100

  // 양끝 칸의 말풍선이 카드 밖으로 삐져나가지 않도록 붙이는 방향을 바꾼다.
  const align =
    index < 2 ? 'left-0' : index > total - 3 ? 'right-0' : 'left-1/2 -translate-x-1/2'

  return (
    // tabIndex: Tab 으로도 같은 값을 볼 수 있어야 한다.
    // 판정 영역은 막대가 아니라 칸 전체다. 얇은 막대를 정확히 겨냥할 필요가 없다.
    <div
      tabIndex={0}
      className="group relative flex h-full flex-1 items-end justify-center outline-none focus-visible:ring-2 focus-visible:ring-ring"
    >
      {entry.cost > 0 && (
        <div
          className="min-h-[3px] w-full max-w-5 animate-grow-up bg-primary transition-opacity duration-200 ease-apple group-hover:opacity-80 group-focus-visible:opacity-80"
          // 칸마다 조금씩 늦게 시작시켜 왼쪽에서 오른쪽으로 훑고 지나가게 한다.
          style={{ height: `${percent}%`, animationDelay: `${index * 45}ms` }}
        />
      )}

      {/* 값을 모든 막대에 적으면 읽히지 않는다. 가장 높은 달 하나만 직접 적는다. */}
      {isPeak && (
        <span
          className="pointer-events-none absolute text-[0.625rem] font-medium tabular-nums text-muted-foreground transition-opacity duration-200 group-hover:opacity-0 group-focus-visible:opacity-0"
          style={{ bottom: `calc(${percent}% + 6px)` }}
        >
          {formatCompact(entry.cost)}
        </span>
      )}

      {/* 값이 먼저, 이름이 나중. 어느 달인지는 이미 알고 숫자를 보러 온다. */}
      <div
        className={`pointer-events-none absolute bottom-full z-20 mb-2 hidden border border-border bg-background px-2.5 py-1.5 whitespace-nowrap group-hover:block group-focus-visible:block ${align}`}
      >
        <p className="text-[0.8125rem] font-medium tabular-nums text-strong">
          {formatWon(entry.cost)}
        </p>
        <p className="text-[0.6875rem] tabular-nums text-muted-foreground">
          {formatMonth(entry.month)} · {entry.count}건
        </p>
        {/* 구성은 표에도 있다. 말풍선은 정보를 보태는 장치지 감췄다 보여주는 장치가 아니다. */}
        {entry.cost > 0 && (
          <p className="text-[0.6875rem] tabular-nums text-faint">
            정비 {formatWon(entry.maintenanceCost)} · 주유 {formatWon(entry.fuelCost)}
          </p>
        )}
      </div>
    </div>
  )
}

/**
 * 정비 종류별 비용.
 *
 * 막대마다 다른 색을 주지 않는다. 정비 종류에는 순서가 없어서 "클수록 진하게"는
 * 막대 길이가 이미 말한 것을 한 번 더 말하는 꼴이다.
 * 다섯 줄짜리 표에 가까워서 값과 건수를 모두 적었고, 그래서 말풍선도 없다.
 */
export function TypeCostChart({ byType }: { byType: TypeCost[] }) {
  const max = Math.max(...byType.map((entry) => entry.cost), 1)

  return (
    <Card>
      <CardHeader>
        <CardTitle>정비 종류별 비용</CardTitle>
        {/* 옆 차트가 '유지비'(정비+주유)로 바뀌면서 범위를 헷갈리기 쉬워졌다.
            주유는 정비 종류가 아니라 여기 들어갈 자리가 없으므로 그 사실을 적어 둔다. */}
        <CardDescription>전체 기간 합계입니다. 유류비는 포함하지 않습니다.</CardDescription>
      </CardHeader>

      <CardContent>
        {byType.length === 0 ? (
          <p className="py-4 text-sm text-muted-foreground">아직 등록된 정비 이력이 없습니다.</p>
        ) : (
          <ul className="flex flex-col gap-5">
            {byType.map((entry, index) => (
              <li key={entry.type} className="flex flex-col gap-2">
                <div className="flex items-baseline justify-between gap-4">
                  <span className="text-[0.9375rem] tracking-[-0.01em] text-strong">
                    {SERVICE_TYPE_LABELS[entry.type]}
                    <span className="ml-2 text-xs tabular-nums text-muted-foreground">
                      {entry.count}건
                    </span>
                  </span>
                  <span className="shrink-0 text-[0.8125rem] tabular-nums text-strong">
                    {formatWon(entry.cost)}
                  </span>
                </div>

                {/* 트랙(옅은 바탕)을 깔아야 "얼마나 찼는지"가 끝 위치만으로도 읽힌다. */}
                <div className="h-2 w-full overflow-hidden bg-sunken">
                  <div
                    className="h-full animate-grow-right bg-primary"
                    style={{
                      width: `${(entry.cost / max) * 100}%`,
                      animationDelay: `${index * 70}ms`,
                    }}
                  />
                </div>
              </li>
            ))}
          </ul>
        )}
      </CardContent>
    </Card>
  )
}
