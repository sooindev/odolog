import { createContext, useContext } from 'react'

// Field 가 안쪽 입력칸에 넘기는 도움말 id
// .ts 분리: 핫 리로드 유지
export const FieldContext = createContext<{ htmlFor: string; hintId?: string } | null>(null)

/** 라벨과 같은 id 의 칸만 도움말을 aria-describedby 로 받음 */
export function useHintId(id: string | undefined) {
  const field = useContext(FieldContext)
  return field !== null && field.htmlFor === id ? field.hintId : undefined
}
