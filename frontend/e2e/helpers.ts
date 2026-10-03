import { expect, type Browser, type Page } from '@playwright/test'

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

/**
 * 콘솔 오류·처리 안 된 예외 모으기. 확인 창은 전부 수락
 * browser.newPage() 로 연 페이지도 같은 감시를 받게 open 으로 열고, 끝나면 closeAll
 */
export function errorGuard() {
  const errors: string[] = []
  const opened: Page[] = []

  function watch(page: Page) {
    page.on('dialog', (dialog) => dialog.accept())
    page.on('console', (message) => {
      // 401·404 같은 예상된 응답은 브라우저가 스스로 찍는 줄. 화면 코드의 오류가 아님
      if (message.type() === 'error' && !message.text().startsWith('Failed to load resource')) {
        errors.push(message.text())
      }
    })
    page.on('pageerror', (error) => errors.push(error.message))
  }

  async function open(browser: Browser) {
    const page = await browser.newPage()
    opened.push(page)
    watch(page)
    return page
  }

  async function closeAll() {
    // 페이지마다 따로 만들어진 컨텍스트까지
    await Promise.all(opened.splice(0).map((page) => page.context().close()))
  }

  function reset() {
    errors.length = 0
  }

  return { errors, watch, open, closeAll, reset }
}
