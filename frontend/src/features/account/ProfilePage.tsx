import { useAuth } from '@/features/auth/context/AuthContext'
import { PasswordForm } from '@/features/auth/components/PasswordForm'
import { ProfileForm } from '@/features/auth/components/ProfileForm'
import { RegionForm } from '@/features/auth/components/RegionForm'
import { AppearanceCard } from '@/features/account/components/AppearanceCard'
import { ExportCard } from '@/features/account/components/ExportCard'
import { WithdrawCard } from '@/features/account/components/WithdrawCard'
import { Page } from '@/shared/ui/layout/page'
import { Section } from '@/shared/ui/layout/section'
import { useI18n } from '@/shared/i18n/I18nContext'

/** /me. 프로필(auth)과 계정 기록 전체(account)를 한 화면에. 백엔드도 account → user */
export function ProfilePage() {
  const { user } = useAuth()
  const { t } = useI18n()

  // null 은 여기서 거르고 폼에는 확정된 user 전달
  if (user === null) {
    return null
  }

  return (
    <Page eyebrow="Account" title={t.profile.title}>
      <Section title={t.profile.account.title} description={t.profile.account.description}>
        <ProfileForm user={user} />
      </Section>

      {/* 계정 성격이라 화면 설정보다 앞 */}
      <Section title={t.profile.password.title} description={t.profile.password.description}>
        <PasswordForm />
      </Section>

      {/* 저장하면 화면 언어·단위가 바로 바뀜 */}
      <Section title={t.profile.region.title} description={t.profile.region.description}>
        <RegionForm user={user} />
      </Section>

      {/* 헤더 토글과 같은 상태 공유. 여기서는 현재 설정 확인용 */}
      <Section title={t.profile.appearance.title} description={t.profile.appearance.description}>
        <AppearanceCard />
      </Section>

      <Section title={t.profile.data.title} description={t.profile.data.description}>
        <ExportCard />
      </Section>

      {/* 되돌릴 수 없는 동작은 맨 아래 */}
      <Section title={t.profile.withdraw.title} description={t.profile.withdraw.description}>
        <WithdrawCard />
      </Section>
    </Page>
  )
}
