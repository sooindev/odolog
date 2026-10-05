import { Link } from 'react-router'

import { useAuth } from '@/features/auth/context/AuthContext'
import { LandingPage } from '@/app/LandingPage'
import { Dashboard, DashboardSkeleton } from '@/features/summary/components/Dashboard'
import { fetchSummary } from '@/features/summary/api/endpoints'
import { Button } from '@/shared/ui/base/button'
import { GaugeMark } from '@/shared/ui/mark'
import { Page } from '@/shared/ui/layout/page'
import { ErrorText, LoadingText } from '@/shared/ui/state'
import { useI18n } from '@/shared/i18n/I18nContext'
import { errorMessage } from '@/shared/i18n/errorMessage'
import { useAsyncData } from '@/shared/lib/hooks/useAsyncData'

/**
 * '/' 의 세 얼굴: 비로그인 → 소개, 0대 → 등록 권유, 1대 이상 → 통계
 * 갈림만 여기. 통계 화면은 features/summary
 */
export function HomePage() {
  const { user, loading } = useAuth()

  // 세션 복구 전 판단 금지. 소개 화면 깜빡임 방지
  if (loading) {
    return <LoadingText className="justify-center py-24" />
  }

  if (user === null) {
    return <LandingPage />
  }

  return <SignedInHome nickname={user.nickname} />
}

function SignedInHome({ nickname }: { nickname: string }) {
  const { t } = useI18n()
  // 모듈 최상단 함수라 useCallback 불필요
  const { data, loading, error } = useAsyncData(fetchSummary)

  if (loading) {
    return <DashboardSkeleton />
  }

  // 오류도 머리말(h1) 유지
  if (error !== null) {
    return (
      <Page eyebrow="Overview" title={t.home.title(nickname)}>
        <ErrorText message={errorMessage(error, t, t.home.loadFailed)} />
      </Page>
    )
  }

  if (data === null || data.vehicleCount === 0) {
    return <EmptyGarage />
  }

  return <Dashboard data={data} nickname={nickname} />
}

/** 로그인 + 차량 0대. 앱을 시작하는 자리의 문구 */
function EmptyGarage() {
  const { t } = useI18n()

  return (
    <Page eyebrow="Garage" title={t.home.empty.title} description={t.home.empty.description}>
      <div className="flex flex-col items-center gap-7 border-y border-border px-5 py-20 text-center sm:gap-8 sm:px-8 sm:py-32">
        <GaugeMark className="size-10 text-muted-foreground" />

        <div className="flex max-w-sm flex-col gap-2">
          <p className="text-section text-strong">{t.home.empty.heading}</p>
          <p className="text-caption leading-relaxed text-muted-foreground">{t.home.empty.body}</p>
        </div>

        <Button render={<Link to="/vehicles/new" />}>{t.home.empty.registerFirst}</Button>
      </div>
    </Page>
  )
}
