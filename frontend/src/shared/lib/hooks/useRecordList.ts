import { useState } from 'react'
import type { Dispatch, RefObject, SetStateAction } from 'react'

import { useI18n } from '@/shared/i18n/I18nContext'
import { errorMessage } from '@/shared/i18n/errorMessage'
import { useFocusReturn } from '@/shared/lib/hooks/useFocusReturn'

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
  addButtonRef,
}: {
  setPage: Dispatch<SetStateAction<number>>
  remove: (recordId: string) => Promise<unknown>
  confirmMessage: string
  failedMessage: string
  /** 저장·삭제 뒤 재조회. 끝날 때까지 기다림 */
  afterChange: () => Promise<unknown>
  /** 새 기록 폼을 닫거나 고친 행이 사라졌을 때 포커스가 돌아갈 추가 버튼 */
  addButtonRef: RefObject<HTMLButtonElement | null>
}) {
  const { t } = useI18n()
  const [editing, setEditing] = useState<Editing<R>>('closed')
  // 조회 실패와 동작 실패 분리
  const [actionError, setActionError] = useState<string | null>(null)
  // 삭제 중인 행 id 들. 하나만 들면 두 번째 삭제가 첫 행의 잠금을 풂
  const [deletingIds, setDeletingIds] = useState<ReadonlySet<string>>(new Set())
  // 지운 행 id. 재조회가 실패하면 받아 둔 쪽에 그 행이 남아 있어서
  const [removedIds, setRemovedIds] = useState<ReadonlySet<string>>(new Set())
  // 방금 고쳐 저장한 행 id. 정렬 자리가 바뀌어 지금 쪽에서 빠졌는지 보려고
  const [savedId, setSavedId] = useState<string | null>(null)
  // 폼을 닫으면 포커스를 연 수정 버튼으로. 새 기록이거나 그 행이 사라졌으면 추가 버튼
  const [opener, setOpener] = useState<HTMLElement | null>(null)
  useFocusReturn(editing !== 'closed', opener, addButtonRef)

  /** 화면에 그릴 행. 지운 행을 뺌 */
  function visible(items: R[]) {
    return removedIds.size === 0 ? items : items.filter((item) => !removedIds.has(item.id))
  }

  /** 고친 행이 다시 읽은 쪽에 없는지. 날짜·주행거리를 바꿔 다른 쪽으로 옮겨진 경우 */
  function movedAway(items: R[]) {
    return savedId !== null && !items.some((item) => item.id === savedId)
  }

  /** 그 행을 지우는 중인지. 버튼 잠금용 */
  function isDeleting(recordId: string) {
    return deletingIds.has(recordId)
  }

  /** 새 기록 폼 열기. 닫으면 추가 버튼으로 돌아옴 */
  function openNew() {
    setOpener(null)
    setEditing('new')
  }

  function close() {
    setEditing('closed')
    setActionError(null)
  }

  /** 행 수정 열기. 작성 중인 새 기록 폼을 덮으면 입력이 사라져 한 번 물음. 행끼리 바꿀 때는 묻지 않음 */
  function edit(record: R, button: HTMLElement) {
    if (editing === 'new' && !window.confirm(t.common.discardNewRecord)) {
      return
    }
    setOpener(button)
    setEditing(record)
    setSavedId(null)
  }

  /** 페이지 이동. 수정 중인 행은 화면에서 사라지므로 닫고, 새 기록 폼은 쪽과 무관해 남김 */
  function changePage(next: SetStateAction<number>) {
    setPage(next)
    setEditing((current) => (current === 'new' ? current : 'closed'))
    setActionError(null)
    setSavedId(null)
  }

  /** 저장 뒤. 새 기록은 첫 장 위쪽이라 첫 장으로(지금 장에 머물면 저장 실패로 오해) */
  function saved(created: boolean) {
    if (created) {
      setPage(0)
    }
    setSavedId(typeof editing === 'object' ? editing.id : null)
    close()
    return afterChange()
  }

  async function handleDelete(recordId: string) {
    if (!window.confirm(confirmMessage)) {
      return
    }

    setDeletingIds((current) => new Set(current).add(recordId))

    try {
      await remove(recordId)
      setRemovedIds((current) => new Set(current).add(recordId))
      // 지운 행의 수정 폼만 닫음. 작성 중이던 새 기록·다른 행의 폼은 입력 그대로
      setEditing((current) => (typeof current === 'object' && current.id === recordId ? 'closed' : current))
      setActionError(null)
      setSavedId(null)
      await afterChange()
    } catch (caught) {
      setActionError(errorMessage(caught, t, failedMessage))
    } finally {
      // 이 행의 잠금만 풂
      setDeletingIds((current) => {
        const next = new Set(current)
        next.delete(recordId)
        return next
      })
    }
  }

  return { editing, setEditing, edit, openNew, actionError, isDeleting, visible, movedAway, close, changePage, saved, handleDelete }
}
