import { useTheme } from '@/shared/theme/ThemeContext'
import { ThemeToggle } from '@/shared/theme/ThemeToggle'
import { Card, CardContent } from '@/shared/ui/base/card'
import { useI18n } from '@/shared/i18n/I18nContext'

export function AppearanceCard() {
  const { theme, resolved } = useTheme()
  const { t } = useI18n()

  // system 일 때 현재 적용 모드 표시
  const detail =
    theme === 'system'
      ? t.profile.appearance.followsSystem(resolved === 'dark')
      : t.profile.appearance.fixed(theme === 'dark')

  return (
    <Card>
      {/*
        좁은 화면은 세로 배치
        min-w-0: 토글 밀림 방지
      */}
      <CardContent className="flex flex-col gap-5 sm:flex-row sm:items-center sm:justify-between sm:gap-4">
        <div className="flex min-w-0 flex-col gap-1">
          <p className="text-body text-strong">{t.profile.appearance.mode}</p>
          <p className="text-caption text-muted-foreground">{detail}</p>
        </div>
        <ThemeToggle className="shrink-0" />
      </CardContent>
    </Card>
  )
}
