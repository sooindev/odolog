import { createCn } from 'cn/config'

/** index.css 의 타입 스케일 토큰. 토큰을 더하면 여기도 */
export const TYPE_SCALE = [
  'eyebrow',
  'title',
  'headline',
  'display',
  'figure',
  'section',
  'lede',
  'body',
  'caption',
  'unit',
  'axis',
] as const

/**
 * 프로젝트 토큰을 아는 cn. 'cn' 패키지를 직접 쓰지 않음
 * 기본 cn 은 text-caption 을 색으로 추정해 text-strong 과 겹치면 크기를 지움
 */
export const cn = createCn({
  extend: { classGroups: { 'font-size': [{ text: [...TYPE_SCALE] }] } },
})
