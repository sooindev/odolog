import { expect, test, type Page } from '@playwright/test'

import { PASSWORD, errorGuard, registerVehicle, signUp, uniqueEmail } from './helpers.ts'

const guard = errorGuard()

test.beforeEach(({ page }) => {
  guard.reset()
  guard.watch(page)
})

test.afterEach(async () => {
  await guard.closeAll()
  expect(guard.errors).toEqual([])
})

/** 제목으로 카드 하나를 고름. 같은 이름의 버튼(수정·삭제)이 여러 카드에 있어서 */
function card(page: Page, title: string) {
  return page.locator('[data-slot="card"]').filter({ has: page.getByRole('heading', { name: title, exact: true }) })
}

/** 서울 기준 오늘에서 n 개월 전(일은 28 이하로 — 월말 보정과 무관하게) */
function monthsAgo(n: number) {
  const [y, m, d] = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Seoul' }).format(new Date()).split('-').map(Number)
  const date = new Date(Date.UTC(y, m - 1 - n, Math.min(d, 28)))
  return date.toISOString().slice(0, 10)
}

test('정비 이력 등록·수정·삭제, 폼을 연 채 다른 행을 고르면 그 행의 값으로 바뀐다', async ({ page }) => {
  await signUp(page, uniqueEmail('maintenance'))
  await registerVehicle(page, '11가1111', 30000)
  const history = card(page, '정비 이력')

  for (const [type, memo] of [
    ['TIRE', '타이어 A'],
    ['BATTERY', '배터리 B'],
  ]) {
    await history.getByRole('button', { name: '이력 추가' }).click()
    await page.locator('#type').selectOption(type)
    await page.locator('#description').fill(memo)
    await history.locator('button[type=submit]').click()
    await expect(page.locator('#description')).toHaveCount(0)
  }
  await expect(history.getByText('타이어 A')).toBeVisible()

  // 한 행의 수정을 연 채 다른 행의 수정 → 폼 내용이 그 행으로(B-44-1)
  await history.getByRole('listitem').filter({ hasText: '타이어 A' }).getByRole('button', { name: '수정' }).click()
  await expect(page.locator('#description')).toHaveValue('타이어 A')
  await history.getByRole('listitem').filter({ hasText: '배터리 B' }).getByRole('button', { name: '수정' }).click()
  await expect(page.locator('#description')).toHaveValue('배터리 B')

  await page.locator('#description').fill('배터리 B 교체')
  await history.locator('button[type=submit]').click()
  await expect(history.getByText('배터리 B 교체')).toBeVisible()
  // 다른 행은 그대로
  await expect(history.getByText('타이어 A')).toBeVisible()

  await history.getByRole('listitem').filter({ hasText: '타이어 A' }).getByRole('button', { name: '삭제' }).click()
  await expect(history.getByText('타이어 A')).toHaveCount(0)
})

test('주행거리를 낮추면 확인을 거쳐 정정된다 — 자리수 오타의 유일한 복구 경로', async ({ page }) => {
  await signUp(page, uniqueEmail('odometer'))
  await registerVehicle(page, '22가2222', 500000)

  await page.locator('#odometer').fill('50000')
  await page.getByRole('button', { name: '갱신' }).click()

  await expect(page.locator('#odometer')).toHaveValue('50000')
  await expect(page.getByText('주행거리 50,000km')).toBeAttached()
})

test('다음 정비가 한 달 안이면 곧 으로 표시되고 차량 목록에도 붙는다', async ({ page }) => {
  await signUp(page, uniqueEmail('soon'))
  await registerVehicle(page, '33가3333', 30000)

  // 엔진오일 6개월 주기. 5개월 전에 갈았으면 다음은 한 달 안
  await page.getByRole('button', { name: '적기', exact: true }).click()
  await page.getByRole('group', { name: '엔진오일' }).getByRole('button', { name: '날짜 지정' }).click()
  await page.locator('#quick-date-ENGINE_OIL').fill(monthsAgo(5))
  await page.getByRole('button', { name: '저장', exact: true }).click()

  await expect(card(page, '다음 정비 시점').getByText('곧', { exact: true })).toBeVisible()
  await page.goto('/vehicles')
  await expect(page.getByText('정비 1건 곧')).toBeVisible()
})

test('홈 대시보드의 최근 활동을 누르면 그 차량으로 간다', async ({ page }) => {
  await signUp(page, uniqueEmail('home'))
  const vehicleUrl = await registerVehicle(page, '44가4444', 10000)
  await page.getByRole('button', { name: '주유 추가' }).click()
  await page.locator('#fuel-odometer').fill('10100')
  await page.locator('#fuel-liters').fill('30')
  await page.locator('#fuel-cost').fill('50000')
  await page.locator('form').filter({ has: page.locator('#fuel-odometer') }).locator('button[type=submit]').click()
  await expect(page.locator('#fuel-odometer')).toHaveCount(0)

  await page.goto('/')
  await expect(page.getByText('Overview')).toBeVisible()
  await card(page, '최근 활동').getByRole('link').first().click()
  await expect(page).toHaveURL(vehicleUrl)
})

test('내보낸 파일을 새 계정에 가져오면 차량과 기록이 그대로 생긴다', async ({ page, browser }) => {
  await signUp(page, uniqueEmail('export'))
  await registerVehicle(page, '55가5555', 20000)
  await page.getByRole('button', { name: '적기', exact: true }).click()
  await page.getByRole('group', { name: '엔진오일' }).getByRole('button', { name: '3개월 전' }).click()
  await page.getByRole('button', { name: '저장', exact: true }).click()
  await expect(card(page, '다음 정비 시점').getByText('엔진오일')).toBeVisible()

  await page.goto('/me')
  const [download] = await Promise.all([
    page.waitForEvent('download'),
    page.getByRole('button', { name: 'JSON 내려받기' }).click(),
  ])
  const file = await download.path()

  const other = await guard.open(browser)
  await signUp(other, uniqueEmail('import'))
  await other.goto('/me')
  await other.locator('input[type=file]').setInputFiles(file)
  await expect(other.getByText('차량 1대와 기록 1건을 넣었습니다.')).toBeVisible()
  await other.goto('/vehicles')
  await expect(other.getByText('55가5555')).toBeVisible()
})

test('탈퇴하면 처음 화면으로 가고 그 계정으로는 로그인할 수 없다', async ({ page }) => {
  const email = uniqueEmail('withdraw')
  await signUp(page, email)
  await page.goto('/me')
  await page.getByRole('button', { name: '회원 탈퇴' }).click()
  await page.locator('#withdraw-password').fill(PASSWORD)
  await page.getByRole('button', { name: '탈퇴하기' }).click()
  await expect(page).toHaveURL(/\/$/)

  await page.goto('/login')
  await page.locator('#email').fill(email)
  await page.locator('#password').fill(PASSWORD)
  await page.getByRole('button', { name: '로그인', exact: true }).last().click()
  await expect(page.getByText('이메일 또는 비밀번호가 올바르지 않습니다.')).toBeVisible()
})
