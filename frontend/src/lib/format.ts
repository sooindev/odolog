/** 45000 → "45,000km" */
export function formatKm(value: number) {
  return `${value.toLocaleString('ko-KR')}km`
}

/** 50000 → "50,000원" */
export function formatWon(value: number) {
  return `${value.toLocaleString('ko-KR')}원`
}
