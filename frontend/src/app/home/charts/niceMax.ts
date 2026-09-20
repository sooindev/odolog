/**
 * 축 눈금 최댓값을 1·2·5 × 10ⁿ 로 올림. 1,873 으로 끝나는 축은 못 읽는다
 * 컴포넌트 파일에서 내보내면 핫 리로드가 깨져 .ts 로 갈라 둔다
 */
export function niceMax(value: number) {
  if (value <= 0) return 1

  const magnitude = 10 ** Math.floor(Math.log10(value))
  const normalized = value / magnitude

  const step = normalized <= 1 ? 1 : normalized <= 2 ? 2 : normalized <= 5 ? 5 : 10

  return step * magnitude
}
