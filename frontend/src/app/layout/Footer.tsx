import { Link } from 'react-router'

import { useI18n } from '@/shared/i18n/context/I18nContext'

/** 모든 화면 맨 아래. 약관·개인정보처리방침은 어디서든 한 번에 닿아야 함 */
export function Footer() {
  const { t } = useI18n()

  return (
    <footer className="border-t border-border">
      <div className="mx-auto flex max-w-[76rem] items-center gap-5 px-5 py-8 pb-[calc(2rem+env(safe-area-inset-bottom))] text-caption text-muted-foreground sm:px-8 lg:px-10">
        <span>{t.app.name}</span>
        <Link to="/privacy" className="transition-opacity duration-200 ease-apple hover:opacity-70">
          {t.footer.privacy}
        </Link>
        <Link to="/terms" className="transition-opacity duration-200 ease-apple hover:opacity-70">
          {t.footer.terms}
        </Link>
      </div>
    </footer>
  )
}
