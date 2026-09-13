import { SERVICE_TYPE_LABELS } from '@/features/maintenance/api/types'
import type { MonthlyCost, TypeCost } from '@/app/homeStats'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/shared/ui/card'
import { formatCompact, formatMonth, formatNumber, formatWon } from '@/shared/lib/format'

/*
 * 홈 화면의 두 차트.
 *
 * **색으로 화려하게 만들지 않는다.** 이 앱의 규칙이 "Accent 하나, 쨍한 색 금지"이고,
 * 차트에서 색을 늘리는 건 보통 정보를 늘리는 게 아니라 노이즈를 늘리는 일이다.
 * 두 차트 모두 **계열이 하나**라 색이 구분할 것이 애초에 없다 — 그래서 범례도 없다
 * (계열이 하나면 제목이 이미 무엇을 그린 것인지 말해 준다).
 * 화려함은 크기(히어로 숫자)·밀도·자라나는 움직임이 만든다.
 *
 * 막대 규격: 두께 24px 이하, 데이터 끝만 4px 둥글게(바닥은 각지게), 눈금선은 1px 실선.
 * 굵은 막대와 점선 눈금은 차트를 시끄럽게 만든다 — 데이터만 소리를 내야 한다.
 */

/** 축 눈금이 1,873 같은 숫자로 끝나지 않도록 1·2·5 × 10ⁿ 중 가장 가까운 위쪽 값으로 올린다. */
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

  return (
    <Card>
      <CardHeader>
        <CardTitle>지난 12개월 정비 비용</CardTitle>
        <CardDescription>달마다 들어간 정비 비용입니다.</CardDescription>
      </CardHeader>

      <CardContent className="flex flex-col gap-8">
        {/*
          히어로 숫자. 한 화면에 하나만 둔다.
          tabular-nums 를 쓰지 않는다 — 모든 숫자를 0 너비로 맞추는 설정이라,
          큰 글씨에서는 1 같은 좁은 글자 주변이 휑하게 벌어져 보인다.
          자릿수를 세로로 맞춰야 하는 '표의 열'에서만 쓸 것.
        */}
        <div className="flex items-baseline gap-2">
          <span className="text-[3rem] leading-none font-semibold tracking-[-0.045em] text-strong">
            {formatNumber(total)}
          </span>
          <span className="text-base text-muted-foreground">원</span>
        </div>

        <figure className="flex flex-col gap-2">
          <div className="relative">
            {/*
              눈금선. h-0 + items-center 라서 각 줄의 선이 정확히 0% / 50% / 100% 위치에 온다.
              (높이가 있으면 글자 높이만큼 선이 밀린다.)
            */}
            <div className="absolute inset-0 flex flex-col justify-between">
              {[max, max / 2, 0].map((tick) => (
                <div key={tick} className="flex h-0 items-center gap-3">
                  <span className="w-10 shrink-0 text-right text-[0.625rem] tabular-nums text-muted-foreground">
                    {formatCompact(tick)}
                  </span>
                  <div className="h-px flex-1 bg-border" />
                </div>
              ))}
            </div>

            {/* z-10: 막대가 눈금선 위로 올라와야 한다. */}
            <div className="relative z-10 ml-[3.25rem] flex h-44 items-end gap-1">
              {monthly.map((entry, index) => (
                <MonthColumn
                  key={entry.month}
                  entry={entry}
                  index={index}
                  total={monthly.length}
                  max={max}
                  isPeak={entry.cost === peak && peak > 0}
                />
              ))}
            </div>
          </div>

          {/* 가로축은 차트 칸 '안'에 둔다. 높이를 고정한 칸 밖으로 밀어내면 축만 잘린다. */}
          <div className="ml-[3.25rem] flex gap-1">
            {monthly.map((entry) => (
              <span
                key={entry.month}
                className="flex-1 text-center text-[0.625rem] tabular-nums text-muted-foreground"
              >
                {Number(entry.month.slice(5))}
              </span>
            ))}
          </div>
          <figcaption className="ml-[3.25rem] text-[0.625rem] text-muted-foreground">월</figcaption>
        </figure>

        {/*
          표로도 볼 수 있게 한다. 마우스를 올려야만 보이는 값은 키보드·스크린리더 사용자에게
          없는 것이나 마찬가지다. <details> 는 브라우저가 접기/펴기와 키보드 조작을 다 해 준다.
        */}
        <details className="group">
          <summary className="w-fit cursor-pointer list-none text-[0.8125rem] text-muted-foreground transition-opacity duration-200 ease-apple hover:opacity-70">
            표로 보기
          </summary>
          <table className="mt-4 w-full text-[0.8125rem]">
            <thead>
              <tr className="border-b border-border text-left text-muted-foreground">
                <th scope="col" className="pb-2 font-medium">
                  월
                </th>
                <th scope="col" className="pb-2 text-right font-medium">
                  건수
                </th>
                <th scope="col" className="pb-2 text-right font-medium">
                  비용
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
                  <td className="py-2 text-right tabular-nums text-strong">
                    {formatWon(entry.cost)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
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
    // tabIndex=0: 마우스 없이 Tab 으로도 같은 값을 볼 수 있어야 한다.
    // 칸 전체가 판정 영역이라 막대보다 훨씬 넓다 — 24px 막대를 정확히 겨냥할 필요가 없다.
    <div
      tabIndex={0}
      className="group relative flex h-full flex-1 items-end justify-center rounded-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
    >
      {entry.cost > 0 && (
        <div
          className="min-h-[3px] w-full max-w-6 animate-grow-up rounded-t-[4px] bg-primary transition-opacity duration-200 ease-apple group-hover:opacity-80 group-focus-visible:opacity-80"
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

      {/* 말풍선은 값이 먼저, 이름이 나중. 읽는 사람은 이미 어느 달인지 알고 숫자를 보러 온다. */}
      <div
        className={`pointer-events-none absolute bottom-full z-20 mb-2 hidden rounded-lg border border-border bg-background px-2.5 py-1.5 whitespace-nowrap group-hover:block group-focus-visible:block ${align}`}
      >
        <p className="text-[0.8125rem] font-medium tabular-nums text-strong">
          {formatWon(entry.cost)}
        </p>
        <p className="text-[0.6875rem] tabular-nums text-muted-foreground">
          {formatMonth(entry.month)} · {entry.count}건
        </p>
      </div>
    </div>
  )
}

/**
 * 정비 종류별 비용.
 *
 * 막대마다 다른 색을 주지 않는다. 정비 종류에는 타고난 순서가 없어서 "큰 것일수록 진하게"는
 * 막대 길이가 이미 말한 것을 색으로 한 번 더 말하는 꼴이고, 색이라는 채널 하나를 그냥 태운다.
 *
 * 값과 건수를 모든 줄에 적었다. 다섯 줄짜리 표에 가까운 형태라 가려진 정보가 없고,
 * 그래서 말풍선도 두지 않았다 — 말풍선은 정보를 보태는 것이지 감췄다 보여주는 장치가 아니다.
 */
export function TypeCostChart({ byType }: { byType: TypeCost[] }) {
  const max = Math.max(...byType.map((entry) => entry.cost), 1)

  return (
    <Card>
      <CardHeader>
        <CardTitle>정비 종류별 비용</CardTitle>
        <CardDescription>전체 기간 합계입니다.</CardDescription>
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
                <div className="h-2 w-full overflow-hidden rounded-[4px] bg-sunken">
                  {/* 오른쪽(데이터가 끝나는 쪽)만 둥글다. 왼쪽은 기준선이라 각지게 둔다. */}
                  <div
                    className="h-full animate-grow-right rounded-r-[4px] bg-primary"
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
