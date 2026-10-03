import type { MonthlyCost, TypeCost } from '@/app/home/homeStats'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/shared/ui/base/card'
import { useI18n } from '@/shared/i18n/I18nContext'
import { niceMax } from '@/app/home/charts/niceMax'

// 홈 차트 둘. 라이브러리 없이 HTML·CSS
// 계열 하나라 범례 없음. 눈금선은 1px 실선(점선은 예측선으로 읽힘)
// 금액은 최소 단위, 통화는 사용자 통화(서버가 같은 통화만 합산)

export function MonthlyCostChart({ monthly, currency }: { monthly: MonthlyCost[]; currency: string }) {
  const { t, f } = useI18n()
  const total = monthly.reduce((acc, entry) => acc + entry.cost, 0)
  const hero = f.moneyParts(total, currency)
  const peak = Math.max(...monthly.map((entry) => entry.cost))
  const max = niceMax(peak)
  // 값을 직접 적는 막대는 최고값 한 칸만
  const peakIndex = peak > 0 ? monthly.findIndex((entry) => entry.cost === peak) : -1

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t.home.monthly.title}</CardTitle>
        <CardDescription>{t.home.monthly.description}</CardDescription>
      </CardHeader>

      <CardContent className="flex flex-col gap-8">
        {/* 히어로 숫자. 맞출 상대가 없어 tabular-nums 미사용 */}
        <div className="flex items-baseline gap-3">
          <span className="text-display text-strong">{hero.value}</span>
          <span className="text-eyebrow text-muted-foreground uppercase">{hero.unit}</span>
        </div>

        <figure className="flex flex-col gap-2">
          <div className="relative">
            {/* 눈금선. h-0 + items-center 로 정확히 0·50·100% 위치 */}
            <div className="absolute inset-0 flex flex-col justify-between">
              {[max, max / 2, 0].map((tick) => (
                <div key={tick} className="flex h-0 items-center gap-3">
                  <span className="w-8 shrink-0 text-right text-axis tabular-nums text-muted-foreground sm:w-10">
                    {/* 전부 0원이면 0 눈금만 표기 */}
                    {peak > 0 || tick === 0 ? f.compactMoney(tick, currency) : ''}
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
                  currency={currency}
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
          <figcaption className="ml-10 text-axis text-muted-foreground sm:ml-[3.25rem]">
            {t.home.monthly.axis}
          </figcaption>
        </figure>

        {/* 말풍선과 같은 값을 표로. 키보드·스크린리더용 */}
        <details className="group">
          <summary className="w-fit cursor-pointer list-none text-caption text-muted-foreground transition-opacity duration-200 ease-apple hover:opacity-70">
            {t.common.showAsTable}
          </summary>
          {/* 열 5개라 좁은 화면에서 가로 스크롤 허용 */}
          {/* overscroll-x-contain: iOS 사파리 뒤로가기 제스처 방지 */}
          <div className="mt-4 overflow-x-auto overscroll-x-contain">
            <table className="w-full min-w-[26rem] text-caption">
              <thead>
                <tr className="border-b border-border text-left text-muted-foreground">
                  <th scope="col" className="pb-2 font-medium">
                    {t.home.monthly.month}
                  </th>
                  <th scope="col" className="pb-2 text-right font-medium">
                    {t.home.monthly.count}
                  </th>
                  <th scope="col" className="pb-2 text-right font-medium">
                    {t.home.monthly.maintenance}
                  </th>
                  <th scope="col" className="pb-2 text-right font-medium">
                    {t.home.monthly.fuel}
                  </th>
                  <th scope="col" className="pb-2 text-right font-medium">
                    {t.home.monthly.total}
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {monthly.map((entry) => (
                  <tr key={entry.month}>
                    <td className="py-2 tabular-nums text-foreground">{f.month(entry.month)}</td>
                    <td className="py-2 text-right tabular-nums text-muted-foreground">
                      {f.number(entry.count)}
                    </td>
                    <td className="py-2 text-right tabular-nums text-muted-foreground">
                      {f.money(entry.maintenanceCost, currency)}
                    </td>
                    <td className="py-2 text-right tabular-nums text-muted-foreground">
                      {f.money(entry.fuelCost, currency)}
                    </td>
                    <td className="py-2 text-right tabular-nums text-strong">
                      {f.money(entry.cost, currency)}
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
  currency,
  isPeak,
}: {
  entry: MonthlyCost
  index: number
  total: number
  max: number
  currency: string
  isPeak: boolean
}) {
  const { t, f } = useI18n()
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
          {f.compactMoney(entry.cost, currency)}
        </span>
      )}

      {/* 값 먼저, 달 이름 나중 */}
      <div
        className={`pointer-events-none absolute bottom-full z-20 mb-2 hidden border border-border bg-background px-2.5 py-1.5 whitespace-nowrap group-hover:block group-focus-visible:block ${align}`}
      >
        <p className="text-caption font-medium tabular-nums text-strong">
          {f.money(entry.cost, currency)}
        </p>
        <p className="text-unit tabular-nums text-muted-foreground">
          {t.home.monthly.tooltipCount(f.month(entry.month), entry.count)}
        </p>
        {/* 구성은 표에도 있음 */}
        {entry.cost > 0 && (
          <p className="text-unit tabular-nums text-muted-foreground">
            {t.home.monthly.breakdown(
              f.money(entry.maintenanceCost, currency),
              f.money(entry.fuelCost, currency),
            )}
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
export function TypeCostChart({ byType, currency }: { byType: TypeCost[]; currency: string }) {
  const { t, f } = useI18n()
  const max = Math.max(...byType.map((entry) => entry.cost), 1)

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t.home.byType.title}</CardTitle>
        {/* 유류비 제외 명시. 옆 차트(정비+주유)와의 혼동 방지 */}
        <CardDescription>{t.home.byType.description}</CardDescription>
      </CardHeader>

      <CardContent>
        {byType.length === 0 ? (
          <p className="py-4 text-caption text-muted-foreground">{t.home.byType.empty}</p>
        ) : (
          <ul className="flex flex-col gap-5">
            {byType.map((entry, index) => (
              <li key={entry.type} className="flex flex-col gap-2">
                <div className="flex items-baseline justify-between gap-4">
                  <span className="text-body text-strong">
                    {t.serviceTypes[entry.type]}
                    <span className="ml-2 text-unit tabular-nums text-muted-foreground">
                      {t.common.records(entry.count)}
                    </span>
                  </span>
                  <span className="shrink-0 text-caption tabular-nums text-strong">
                    {f.money(entry.cost, currency)}
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
