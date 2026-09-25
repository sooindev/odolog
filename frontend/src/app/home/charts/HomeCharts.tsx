import { SERVICE_TYPE_LABELS } from '@/features/maintenance/api/types/types'
import type { MonthlyCost, TypeCost } from '@/app/home/stats/homeStats'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/shared/ui/base/card'
import { formatCompact, formatMonth, formatNumber, formatWon } from '@/shared/lib/format/format'
import { niceMax } from '@/app/home/charts/niceMax'

/*
 * 홈 차트 둘. 라이브러리 없이 HTML·CSS 로만
 * 계열이 하나라 색이 구분할 것이 없음 → 범례도 없음
 * 막대는 얇게, 눈금선은 1px 실선 (점선은 "예측"이나 "임계선"으로 읽힘)
 */

export function MonthlyCostChart({ monthly }: { monthly: MonthlyCost[] }) {
  const total = monthly.reduce((acc, entry) => acc + entry.cost, 0)
  const peak = Math.max(...monthly.map((entry) => entry.cost))
  const max = niceMax(peak)
  // 값을 직접 적는 막대는 하나뿐. 동점이면 최고값을 가진 첫 칸만
  const peakIndex = peak > 0 ? monthly.findIndex((entry) => entry.cost === peak) : -1

  return (
    <Card>
      <CardHeader>
        <CardTitle>지난 12개월 유지비</CardTitle>
        <CardDescription>달마다 들어간 정비비와 유류비의 합입니다.</CardDescription>
      </CardHeader>

      <CardContent className="flex flex-col gap-8">
        {/* 히어로 숫자. tabular-nums 금지 — 큰 글씨에서는 좁은 글자 주변이 휑하게 벌어짐
            세로로 자릿수를 맞출 상대가 있을 때만 */}
        <div className="flex items-baseline gap-3">
          <span className="text-display text-strong">{formatNumber(total)}</span>
          <span className="text-eyebrow text-muted-foreground uppercase">원</span>
        </div>

        <figure className="flex flex-col gap-2">
          <div className="relative">
            {/* 눈금선. h-0 + items-center 라야 정확히 0/50/100% 에 위치
                높이가 있으면 글자 높이만큼 밀림 */}
            <div className="absolute inset-0 flex flex-col justify-between">
              {[max, max / 2, 0].map((tick) => (
                <div key={tick} className="flex h-0 items-center gap-3">
                  <span className="w-8 shrink-0 text-right text-axis tabular-nums text-muted-foreground sm:w-10">
                    {formatCompact(tick)}
                  </span>
                  <div className="h-px flex-1 bg-border" />
                </div>
              ))}
            </div>

            {/* z-10 — 막대가 눈금선 위로 */}
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

          {/* 가로축은 차트 칸 '안'에. 높이를 고정한 칸 밖으로 밀어내면 축만 잘림 */}
          <div className="ml-10 flex gap-0.5 sm:ml-[3.25rem] sm:gap-1">
            {monthly.map((entry, index) => (
              // 좁은 화면은 칸 하나가 20px 남짓이라 12개를 다 적으면 숫자가 붙음
              // 홀수 칸만 남겨 간격을 두 배로. 막대는 12개 그대로
              <span
                key={entry.month}
                className={`flex-1 text-center text-axis tabular-nums text-muted-foreground ${
                  index % 2 === 1 ? 'invisible sm:visible' : ''
                }`}
              >
                {Number(entry.month.slice(5))}
              </span>
            ))}
          </div>
          <figcaption className="ml-10 text-axis text-muted-foreground sm:ml-[3.25rem]">월</figcaption>
        </figure>

        {/* 마우스를 올려야만 보이는 값은 키보드·스크린리더 사용자에게 없는 것이나 같다.
            <details> 는 접기/펴기와 키보드 조작을 브라우저가 해 준다. */}
        <details className="group">
          <summary className="w-fit cursor-pointer list-none text-caption text-muted-foreground transition-opacity duration-200 ease-apple hover:opacity-70">
            표로 보기
          </summary>
          {/* 열이 5개라 좁은 화면에서는 넘침. 표는 가로 스크롤 허용 예외 —
              글이 담긴 열을 좁히는 것보다 옆으로 미는 편이 나음 */}
          {/* overscroll-x-contain — iOS 사파리는 가로 스크롤이 끝에 닿으면 뒤로가기 제스처로 넘어감 */}
          <div className="mt-4 overflow-x-auto overscroll-x-contain">
            <table className="w-full min-w-[26rem] text-caption">
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

  // 양끝 칸은 말풍선이 카드 밖으로 나가므로 붙이는 방향 반전
  const align =
    index < 2 ? 'left-0' : index > total - 3 ? 'right-0' : 'left-1/2 -translate-x-1/2'

  return (
    // tabIndex — 마우스 없이도 같은 값에 닿아야 함
    // 판정 영역은 막대가 아니라 칸 전체. 얇은 막대를 겨냥할 필요가 없음
    <div
      tabIndex={0}
      className="group relative flex h-full flex-1 items-end justify-center outline-none focus-visible:ring-2 focus-visible:ring-ring"
    >
      {entry.cost > 0 && (
        <div
          className="min-h-[3px] w-full max-w-5 animate-grow-up bg-primary transition-opacity duration-200 ease-apple group-hover:opacity-80 group-focus-visible:opacity-80"
          // 칸마다 지연을 줘 왼쪽에서 오른쪽으로 훑고 지나가게
          style={{ height: `${percent}%`, animationDelay: `${index * 45}ms` }}
        />
      )}

      {/* 모든 막대에 값을 적으면 읽히지 않음. 가장 높은 달 하나만 */}
      {isPeak && (
        <span
          className="pointer-events-none absolute text-axis font-medium tabular-nums text-muted-foreground transition-opacity duration-200 group-hover:opacity-0 group-focus-visible:opacity-0"
          style={{ bottom: `calc(${percent}% + 6px)` }}
        >
          {formatCompact(entry.cost)}
        </span>
      )}

      {/* 값이 먼저, 이름이 나중 — 어느 달인지는 이미 알고 숫자를 보러 옴 */}
      <div
        className={`pointer-events-none absolute bottom-full z-20 mb-2 hidden border border-border bg-background px-2.5 py-1.5 whitespace-nowrap group-hover:block group-focus-visible:block ${align}`}
      >
        <p className="text-caption font-medium tabular-nums text-strong">
          {formatWon(entry.cost)}
        </p>
        <p className="text-unit tabular-nums text-muted-foreground">
          {formatMonth(entry.month)} · {entry.count}건
        </p>
        {/* 구성은 표에도 있음. 말풍선은 정보를 보태는 장치지 감췄다 보여주는 장치가 아님 */}
        {entry.cost > 0 && (
          <p className="text-unit tabular-nums text-muted-foreground">
            정비 {formatWon(entry.maintenanceCost)} · 주유 {formatWon(entry.fuelCost)}
          </p>
        )}
      </div>
    </div>
  )
}

/**
 * 정비 종류별 비용
 * 막대마다 다른 색을 주지 않음 — 정비 종류에는 순서가 없어 "클수록 진하게"가 채널 낭비
 * 표에 가까워 값과 건수를 모두 적었고 그래서 말풍선도 없음
 */
export function TypeCostChart({ byType }: { byType: TypeCost[] }) {
  const max = Math.max(...byType.map((entry) => entry.cost), 1)

  return (
    <Card>
      <CardHeader>
        <CardTitle>정비 종류별 비용</CardTitle>
        {/* 옆 차트가 '유지비'(정비+주유)가 되면서 범위를 헷갈리기 쉬워짐
            주유는 정비 종류가 아니라 여기 들어갈 자리가 없음 */}
        <CardDescription>전체 기간 합계입니다. 유류비는 포함하지 않습니다.</CardDescription>
      </CardHeader>

      <CardContent>
        {byType.length === 0 ? (
          <p className="py-4 text-caption text-muted-foreground">아직 등록된 정비 이력이 없습니다.</p>
        ) : (
          <ul className="flex flex-col gap-5">
            {byType.map((entry, index) => (
              <li key={entry.type} className="flex flex-col gap-2">
                <div className="flex items-baseline justify-between gap-4">
                  <span className="text-body text-strong">
                    {SERVICE_TYPE_LABELS[entry.type]}
                    <span className="ml-2 text-unit tabular-nums text-muted-foreground">
                      {entry.count}건
                    </span>
                  </span>
                  <span className="shrink-0 text-caption tabular-nums text-strong">
                    {formatWon(entry.cost)}
                  </span>
                </div>

                {/* 트랙(옅은 바탕)이 있어야 "얼마나 찼는지"가 끝 위치만으로 읽힘 */}
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
