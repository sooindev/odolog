import { expect, test } from '@playwright/test'

import { PASSWORD, errorGuard, registerVehicle, signUp, uniqueEmail } from './helpers.ts'

// 확인 창은 전부 "확인". 취소 경로는 각 테스트에서 따로
// 콘솔 오류·처리 안 된 예외가 하나라도 나면 실패(확인 목록 A-3 "콘솔에 빨간 줄이 새로 생기지 않는다")
const guard = errorGuard()

test.beforeEach(({ page }) => {
  guard.reset()
  guard.watch(page)
})

test.afterEach(async () => {
  await guard.closeAll()
  expect(guard.errors).toEqual([])
})

test('가입 → 차량 등록 → 기억나는 정비 → 주유 두 번으로 첫 연비', async ({ page }) => {
  await signUp(page, uniqueEmail('core'))
  await expect(page.getByText('아직 등록된 차량이 없습니다')).toBeVisible()

  await registerVehicle(page, '12가3456', 45000)
  // 히어로 주행거리가 0 이 아니라 적은 값
  await expect(page.getByText('45,000').first()).toBeVisible()
  await expect(page.getByText('4단계 중 1단계 완료')).toBeVisible()

  // 엔진오일 6개월 전 → 주기 6개월이라 바로 지남
  await page.getByRole('button', { name: '적기', exact: true }).click()
  await page.getByRole('group', { name: '엔진오일' }).getByRole('button', { name: '6개월 전' }).click()
  await page.getByRole('button', { name: '저장', exact: true }).click()
  await expect(page.getByText('지남', { exact: true })).toBeVisible()
  await expect(page.getByText('4단계 중 2단계 완료')).toBeVisible()

  // 첫 주유는 기준점, 두 번째에서 500km ÷ 25L = 20.00 km/L
  for (const [odometer, liters, cost] of [
    [45100, '30', '50000'],
    [45600, '25', '45000'],
  ]) {
    await page.getByRole('button', { name: '주유 추가' }).click()
    await page.locator('#fuel-odometer').fill(String(odometer))
    await page.locator('#fuel-liters').fill(liters as string)
    await page.locator('#fuel-cost').fill(cost as string)
    await page.locator('form').filter({ has: page.locator('#fuel-odometer') }).locator('button[type=submit]').click()
    await expect(page.locator('#fuel-odometer')).toHaveCount(0)
  }
  await expect(page.getByText(/20\.00/).first()).toBeVisible()
  // 주유가 차량 주행거리를 따라 올림
  await expect(page.getByText('45,600').first()).toBeVisible()
})

test('남의 차량은 404 와 같은 화면 — 존재 여부를 알려주지 않는다', async ({ browser }) => {
  const owner = await guard.open(browser)
  await signUp(owner, uniqueEmail('owner'))
  const url = await registerVehicle(owner, '34나5678', 1000)

  const stranger = await guard.open(browser)
  await signUp(stranger, uniqueEmail('stranger'))
  await stranger.goto(url)
  await expect(stranger.getByText('존재하지 않는 차량입니다.')).toBeVisible()
  // 목록에도 안 보임
  await stranger.goto('/vehicles')
  await expect(stranger.getByText('아직 등록된 차량이 없습니다')).toBeVisible()
})

test('로그아웃하면 보호된 화면은 로그인으로, 다시 로그인하면 가려던 곳으로', async ({ page }) => {
  const email = uniqueEmail('logout')
  await signUp(page, email)
  await page.getByRole('button', { name: '로그아웃' }).click()

  await page.goto('/vehicles')
  await expect(page).toHaveURL(/\/login$/)
  await page.locator('#email').fill(email)
  await page.locator('#password').fill(PASSWORD)
  await page.getByRole('button', { name: '로그인', exact: true }).last().click()
  await expect(page).toHaveURL(/\/vehicles$/)
})

test('새로고침해도 로그인이 유지된다 — 세션은 DB 에 있다', async ({ page }) => {
  await signUp(page, uniqueEmail('reload'))
  await page.reload()
  await expect(page).toHaveURL(/\/vehicles$/)
  await expect(page.getByRole('button', { name: '로그아웃' })).toBeVisible()
  const cookies = await page.context().cookies()
  expect(cookies.map((cookie) => cookie.name)).toContain('JSESSIONID')
})

test('현재 비밀번호를 틀려도 로그아웃되지 않는다', async ({ page }) => {
  await signUp(page, uniqueEmail('password'))
  await page.goto('/me')
  await page.locator('#current-password').fill('wrong-password')
  await page.locator('#new-password').fill('newpassword1234')
  await page.locator('#confirm-password').fill('newpassword1234')
  await page.getByRole('button', { name: '비밀번호 변경' }).click()

  await expect(page.getByText('현재 비밀번호가 올바르지 않습니다.')).toBeVisible()
  await expect(page).toHaveURL(/\/me$/)
  await expect(page.getByRole('button', { name: '로그아웃' })).toBeVisible()
})

test('차량을 지우면 확인 뒤 목록에서 사라진다', async ({ page }) => {
  await signUp(page, uniqueEmail('delete'))
  await registerVehicle(page, '56다7890', 2000)
  await page.getByRole('button', { name: '차량 삭제' }).click()

  await expect(page).toHaveURL(/\/vehicles$/)
  await expect(page.getByText('아직 등록된 차량이 없습니다')).toBeVisible()
})

test('같은 탭에서 다른 계정으로 바꿔 로그인하면 앞 계정의 차량이 캐시로 보이지 않는다', async ({ browser, page }) => {
  // 두 번째 계정은 다른 탭에서 미리 가입
  const otherEmail = uniqueEmail('switch-b')
  const other = await guard.open(browser)
  await signUp(other, otherEmail)

  await signUp(page, uniqueEmail('switch-a'))
  const url = await registerVehicle(page, '56다7890', 2000)
  await expect(page.getByText('56다7890').first()).toBeVisible()

  // 새로고침 없이 로그아웃 → 로그인. 페이지를 다시 읽으면 캐시가 저절로 비어 확인이 안 된다
  await page.getByRole('button', { name: '로그아웃' }).click()
  await expect(page).toHaveURL(/\/login$/)
  await page.locator('#email').fill(otherEmail)
  await page.locator('#password').fill(PASSWORD)
  // 차량 응답을 늦춰 둔다. 캐시가 남아 있으면 그동안 앞 계정의 차량이 보인다
  const vehicleApi = new RegExp(`/api/vehicles/${new URL(url).pathname.split('/').pop()}$`)
  await page.route(vehicleApi, async (route) => {
    await new Promise((resolve) => setTimeout(resolve, 2000))
    await route.continue()
  })
  await page.getByRole('button', { name: '로그인', exact: true }).last().click()
  await expect(page).toHaveURL(url)
  // 늦춘 응답이 오기 전. 이 사이에 보이는 것은 캐시뿐
  // 재시도하는 단언은 늦춘 응답이 오면 통과해 버리므로 지금 한 번만 센다
  await page.waitForTimeout(1000)
  expect(await page.getByText('56다7890').count()).toBe(0)

  // 로그인 화면이 기억해 둔 앞 계정의 차량 주소로 돌아온다 — 새로고침 없는 화면 안 이동
  await expect(page).toHaveURL(url)
  await expect(page.getByText('존재하지 않는 차량입니다.')).toBeVisible()
  await expect(page.getByText('56다7890')).toHaveCount(0)
})
