import { expect, test } from '@playwright/test'

import { PASSWORD, errorGuard, uniqueEmail } from './helpers.ts'

// 미국 브라우저로 가입하면 영어·마일·달러. 저장은 km 라도 화면은 마일(7-H 일부)
test.use({ locale: 'en-US', timezoneId: 'America/Los_Angeles' })

// 콘솔 오류가 하나라도 나면 실패. 다른 spec 과 같은 기준
const guard = errorGuard()

test.beforeEach(({ page }) => {
  guard.reset()
  guard.watch(page)
})

test.afterEach(async () => {
  await guard.closeAll()
  expect(guard.errors).toEqual([])
})

test('영어 브라우저로 가입하면 화면이 영어이고 주행거리는 마일이다', async ({ page }) => {
  await page.goto('/signup')
  await page.locator('#email').fill(uniqueEmail('english'))
  await page.locator('#password').fill(PASSWORD)
  await page.locator('#nickname').fill('driver')
  await page.getByRole('button', { name: 'Create account' }).click()
  await expect(page).toHaveURL(/\/vehicles$/)

  await page.goto('/vehicles/new')
  await page.locator('#plateNumber').fill('7ABC123')
  await page.locator('#manufacturer').fill('Ford')
  await page.locator('#modelName').fill('F-150')
  await page.locator('#modelYear').fill('2021')
  await page.locator('#odometer').fill('10000')
  await page.getByRole('button', { name: 'Add', exact: true }).click()
  await expect(page).toHaveURL(/\/vehicles\/[A-Za-z0-9]{12}$/)

  // 10,000 mi 로 받아 km 로 저장했다가 다시 마일로 보임(왕복 반올림이 원래 값)
  await expect(page.locator('#odometer')).toHaveValue('10000')
  await expect(page.getByText('mi', { exact: true }).first()).toBeVisible()
})
