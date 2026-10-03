import { Link } from 'react-router'

import { useI18n } from '@/shared/i18n/I18nContext'
import { NoticeText } from '@/shared/ui/state'
import { Page } from '@/shared/ui/layout/page'
import { Section } from '@/shared/ui/layout/section'

/** 공개 전 초안. 운영자 정보가 채워지면 날짜와 안내를 함께 갱신 */
const UPDATED = '2026-09-29'

/**
 * 개인정보처리방침·이용약관. 문구는 사전(t.legal)에서
 * 로그인과 무관하게 열리는 문서라 app/ 소속
 */
export function LegalPage({ kind }: { kind: 'privacy' | 'terms' }) {
  const { t, f } = useI18n()
  const doc = t.legal[kind]

  return (
    <Page eyebrow={t.legal.eyebrow} title={doc.title} description={t.legal.updated(f.date(UPDATED))}>
      <NoticeText message={t.legal.draftNotice} />

      {doc.sections.map((section) => (
        <Section key={section.heading} title={section.heading}>
          {/* 글 열 폭 제한. 읽는 문서 */}
          <div className="flex max-w-[65ch] flex-col gap-3">
            {section.body.map((paragraph) => (
              <p key={paragraph} className="text-body leading-relaxed text-foreground">
                {paragraph}
              </p>
            ))}
          </div>
        </Section>
      ))}

      <p className="text-caption text-muted-foreground">
        <Link
          to={kind === 'privacy' ? '/terms' : '/privacy'}
          className="transition-opacity duration-200 ease-apple hover:opacity-70"
        >
          {kind === 'privacy' ? t.footer.terms : t.footer.privacy}
        </Link>
      </p>
    </Page>
  )
}
