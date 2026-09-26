/**
 * 축 최댓값을 1·2·5 × 10ⁿ 로 올림
 * 컴포넌트 파일과 분리. 핫 리로드 유지
 */
export function niceMax(value: number) {
  if (value <= 0) return 1

  const magnitude = 10 ** Math.floor(Math.log10(value))
  const normalized = value / magnitude

  const step = normalized <= 1 ? 1 : normalized <= 2 ? 2 : normalized <= 5 ? 5 : 10

  return step * magnitude
}
