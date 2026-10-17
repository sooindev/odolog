import { Component } from 'react'
import type { ReactNode } from 'react'

import { useI18n } from '@/shared/i18n/I18nContext'
import { Button } from '@/shared/ui/base/button'
import { ErrorText } from '@/shared/ui/state'

/**
 * 화면을 그리다 실패하면 흰 화면 대신 안내와 새로고침
 * 주 대상은 지연 로딩한 화면 조각을 못 받은 경우(오프라인, 배포로 파일 이름이 바뀜)
 * 다른 주소로 가면 다시 그리도록 부르는 쪽이 pathname 으로 key 를 줌
 * 실패한 조각은 lazy 가 기억해 같은 화면은 새로고침해야 다시 받음
 */
export class ScreenErrorBoundary extends Component<{ children: ReactNode }, { failed: boolean }> {
  state = { failed: false }

  static getDerivedStateFromError() {
    return { failed: true }
  }

  render() {
    return this.state.failed ? <ScreenFailed /> : this.props.children
  }
}

/** 새로고침이 새 index.html 과 새 파일 이름을 받아 옴 */
function ScreenFailed() {
  const { t } = useI18n()

  return (
    <div className="mx-auto flex max-w-md flex-col items-center gap-4 py-24">
      <ErrorText message={t.common.screenFailed} />
      <Button onClick={() => window.location.reload()}>{t.common.reload}</Button>
    </div>
  )
}
