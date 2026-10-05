import { useI18n } from '@/shared/i18n/I18nContext'
import { fromKm } from '@/shared/lib/units'
import { useCountUp } from '@/shared/lib/hooks/useCountUp'

/**
 * 주행거리 히어로 숫자. 값이 바뀔 때만 굴러감
 * tabular-nums 는 굴러가는 동안만
 */
export function OdometerHero({ odometer }: { odometer: number }) {
  const { t, f, unitSystem } = useI18n()
  // 화면 단위로 바꾼 뒤 굴림. km 로 굴리면 마일 화면에서 중간값이 튐
  const { value, running } = useCountUp(Math.round(fromKm(unitSystem, odometer)))

  return (
    <div className="flex flex-col gap-4 border-b border-border pb-8">
      <p className="text-eyebrow text-muted-foreground uppercase">Odometer</p>
      {/* 굴러가는 숫자는 aria-hidden, 확정 값만 aria-live 로 한 번 안내 */}
      <p
        aria-hidden="true"
        className={`flex items-baseline gap-3 text-display text-strong ${
          running ? 'tabular-nums' : ''
        }`}
      >
        {f.number(value)}
        <span className="text-eyebrow text-muted-foreground uppercase">{f.distanceUnit}</span>
      </p>
      <p className="sr-only" aria-live="polite" aria-atomic="true">
        {t.vehicles.detail.odometerAnnounce(f.distance(odometer))}
      </p>
    </div>
  )
}
