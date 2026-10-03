import { expect, type Page } from '@playwright/test'

/** 테스트마다 새 계정. 같은 DB 를 쓰는 흐름끼리 섞이지 않게 */
export function uniqueEmail(label: string) {
  return `${label}-${Date.now()}-${Math.floor(Math.random() * 1e6)}@e2e.test`
}

export const PASSWORD = 'password1234'

export async function signUp(page: Page, email: string, nickname = 'e2e') {
  await page.goto('/signup')
  await page.locator('#email').fill(email)
  await page.locator('#password').fill(PASSWORD)
  await page.locator('#nickname').fill(nickname)
  await page.getByRole('button', { name: '회원가입', exact: true }).click()
  // 가입 뒤 이어서 로그인까지 자동
  await expect(page).toHaveURL(/\/vehicles$/)
}

export async function registerVehicle(page: Page, plate: string, odometer: number) {
  await page.goto('/vehicles/new')
  await page.locator('#plateNumber').fill(plate)
  await page.locator('#manufacturer').fill('기아')
  await page.locator('#modelName').fill('카니발')
  await page.locator('#modelYear').fill('2020')
  await page.locator('#odometer').fill(String(odometer))
  await page.getByRole('button', { name: '등록', exact: true }).click()
  // 주소는 숫자 PK 가 아니라 12자 공개 id
  await expect(page).toHaveURL(/\/vehicles\/[A-Za-z0-9]{12}$/)
  return page.url()
}
