import { useState } from 'react'
import type { Dispatch, SetStateAction } from 'react'

import { useI18n } from '@/shared/i18n/I18nContext'
import { errorMessage } from '@/shared/i18n/errorMessage'

/** 'closed' | 'new' | 수정할 기록 */
export type Editing<R> = 'closed' | 'new' | R

/**
 * 기록 목록 카드 공통 상태. 정비 이력·주유 기록 공용
 * 폼 열림·동작 실패 문구·삭제 중인 행. 빈 페이지 복귀는 usePageInRange 몫
 */
export function useRecordList<R extends { id: string }>({
  setPage,
  remove,
  confirmMessage,
  failedMessage,
  afterChange,
}: {
  setPage: Dispatch<SetStateAction<number>>
  remove: (recordId: string) => Promise<unknown>
  confirmMessage: string
  failedMessage: string
  /** 저장·삭제 뒤 재조회. 끝날 때까지 기다림 */
  afterChange: () => Promise<unknown>
}) {
  const { t } = useI18n()
  const [editing, setEditing] = useState<Editing<R>>('closed')
  // 조회 실패와 동작 실패 분리
  const [actionError, setActionError] = useState<string | null>(null)
  // 삭제 중인 행 id
  const [deletingId, setDeletingId] = useState<string | null>(null)

  function close() {
    setEditing('closed')
    setActionError(null)
  }

  /** 페이지 이동. 수정 중인 행이 화면에서 사라지므로 폼·문구도 닫음 */
  function changePage(next: SetStateAction<number>) {
    setPage(next)
    close()
  }

  /** 저장 뒤. 새 기록은 첫 장 위쪽이라 첫 장으로(지금 장에 머물면 저장 실패로 오해) */
  function saved(created: boolean) {
    if (created) {
      setPage(0)
    }
    close()
    return afterChange()
  }

  async function handleDelete(recordId: string) {
    if (!window.confirm(confirmMessage)) {
      return
    }

    setDeletingId(recordId)

    try {
      await remove(recordId)
      close()
      await afterChange()
    } catch (caught) {
      setActionError(errorMessage(caught, t, failedMessage))
    } finally {
      // 먼저 끝난 삭제가 다른 행의 잠금을 풀지 않게
      setDeletingId((current) => (current === recordId ? null : current))
    }
  }

  return { editing, setEditing, actionError, deletingId, close, changePage, saved, handleDelete }
}
