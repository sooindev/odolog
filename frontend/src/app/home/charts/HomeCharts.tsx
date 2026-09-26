import { SERVICE_TYPE_LABELS } from '@/features/maintenance/api/types/types'
import type { MonthlyCost, TypeCost } from '@/app/home/stats/homeStats'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/shared/ui/base/card'
import { formatCompact, formatMonth, formatNumber, formatWon } from '@/shared/lib/format/format'
import { niceMax } from '@/app/home/charts/niceMax'

// 홈 차트 둘. 라이브러리 없이 HTML·CSS
// 계열 하나라 범례 없음. 눈금선은 1px 실선(점선은 예측선으로 읽힘)

export function MonthlyCostChart({ monthly }: { monthly: MonthlyCost[] }) {
  const total = monthly.reduce((acc, entry) => acc + entry.cost, 0)
  const peak = Math.max(...monthly.map((entry) => entry.cost))
  const max = niceMax(peak)
  // 값을 직접 적는 막대는 최고값 한 칸만
  const peakIndex = peak > 0 ? monthly.findIndex((entry) => entry.cost === peak) : -1

  return (
    <Card>
      <CardHeader>
        <CardTitle>지난 12개월 유지비</CardTitle>
        <CardDescription>달마다 들어간 정비비와 유류비의 합입니다.</CardDescription>
      </CardHeader>

      <CardContent className="flex flex-col gap-8">
        {/* 히어로 숫자. 맞출 상대가 없어 tabular-nums 미사용 */}
        <div className="flex items-baseline gap-3">
          <span className="text-display text-strong">{formatNumber(total)}</span>
          <span className="text-eyebrow text-muted-foreground uppercase">원</span>
        </div>

        <figure className="flex flex-col gap-2">
          <div className="relative">
            {/* 눈금선. h-0 + items-center 로 정확히 0·50·100% 위치 */}
            <div className="absolute inset-0 flex flex-col justify-between">
              {[max, max / 2, 0].map((tick) => (
                <div key={tick} className="flex h-0 items-center gap-3">
                  <span className="w-8 shrink-0 text-right text-axis tabular-nums text-muted-foreground sm:w-10">
                    {/* 전부 0원이면 0 눈금만 표기 */}
                    {peak > 0 || tick === 0 ? formatCompact(tick) : ''}
                  </span>
                  <div className="h-px flex-1 bg-border" />
                </div>
              ))}
            </div>

            {/* 막대를 눈금선 위로 */}
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

          {/* 가로축은 차트 칸 안쪽. 밖이면 축만 잘림 */}
          <div className="ml-10 flex gap-0.5 sm:ml-[3.25rem] sm:gap-1">
            {monthly.map((entry, index) => (
              // 좁은 화면은 홀수 칸 라벨만. 막대는 12개 유지
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

        {/* 말풍선과 같은 값을 표로. 키보드·스크린리더용 */}
        <details className="group">
          <summary className="w-fit cursor-pointer list-none text-caption text-muted-foreground transition-opacity duration-200 ease-apple hover:opacity-70">
            표로 보기
          </summary>
          {/* 열 5개라 좁은 화면에서 가로 스크롤 허용 */}
          {/* overscroll-x-contain: iOS 사파리 뒤로가기 제스처 방지 */}
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

  // 양끝 칸은 말풍선 방향 반전. 카드 밖 이탈 방지
  const align =
    index < 2 ? 'left-0' : index > total - 3 ? 'right-0' : 'left-1/2 -translate-x-1/2'

  return (
    // tabIndex: 키보드로 같은 값 접근
    // 판정 영역은 막대가 아닌 칸 전체
    <div
      tabIndex={0}
      className="group relative flex h-full flex-1 items-end justify-center outline-none focus-visible:ring-2 focus-visible:ring-ring"
    >
      {entry.cost > 0 && (
        <div
          className="min-h-[3px] w-full max-w-5 animate-grow-up bg-primary transition-opacity duration-200 ease-apple group-hover:opacity-80 group-focus-visible:opacity-80"
          // 칸마다 지연. 왼쪽에서 오른쪽으로
          style={{ height: `${percent}%`, animationDelay: `${index * 45}ms` }}
        />
      )}

      {/* 값 표기는 가장 높은 달 하나만 */}
      {isPeak && (
        <span
          className="pointer-events-none absolute text-axis font-medium tabular-nums text-muted-foreground transition-opacity duration-200 group-hover:opacity-0 group-focus-visible:opacity-0"
          style={{ bottom: `calc(${percent}% + 6px)` }}
        >
          {formatCompact(entry.cost)}
        </span>
      )}

      {/* 값 먼저, 달 이름 나중 */}
      <div
        className={`pointer-events-none absolute bottom-full z-20 mb-2 hidden border border-border bg-background px-2.5 py-1.5 whitespace-nowrap group-hover:block group-focus-visible:block ${align}`}
      >
        <p className="text-caption font-medium tabular-nums text-strong">
          {formatWon(entry.cost)}
        </p>
        <p className="text-unit tabular-nums text-muted-foreground">
          {formatMonth(entry.month)} · {entry.count}건
        </p>
        {/* 구성은 표에도 있음 */}
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
 * 순서 없는 항목이라 색 구분 없음. 값·건수 직접 표기라 말풍선 없음
 */
export function TypeCostChart({ byType }: { byType: TypeCost[] }) {
  const max = Math.max(...byType.map((entry) => entry.cost), 1)

  return (
    <Card>
      <CardHeader>
        <CardTitle>정비 종류별 비용</CardTitle>
        {/* 유류비 제외 명시. 옆 차트(정비+주유)와의 혼동 방지 */}
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

                {/* 옅은 트랙. 끝 위치만으로 비율 파악 */}
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
