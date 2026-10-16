import { test, expect, type Page } from '@playwright/test'

const suffix = Date.now()
const brand = { email: `e2e-brand-${suffix}@test.com`, password: 'Passw0rd!', name: 'E2E Brand' }
const inf = { email: `e2e-inf-${suffix}@test.com`, password: 'Passw0rd!', name: 'E2E Influencer' }
const campaignTitle = `E2E Summer Drop ${suffix}`
const creatorName = `E2E Creator ${suffix}`

async function register(page: Page, who: typeof brand, role: 'BRAND' | 'INFLUENCER') {
  await page.goto('/register')
  await page.getByLabel('Full name').fill(who.name)
  await page.getByLabel('Email').fill(who.email)
  await page.getByLabel('Password').fill(who.password)
  await page.getByLabel('I am a').selectOption(role)
  await page.getByRole('button', { name: 'Register' }).click()
  await expect(page).toHaveURL('/')
}

async function login(page: Page, email: string, password: string) {
  await page.goto('/login')
  await page.locator('input').nth(0).fill(email)
  await page.locator('input[type="password"]').fill(password)
  await page.getByRole('button', { name: 'Sign in' }).click()
  await expect(page).toHaveURL('/')
}

test.describe.serial('Ad-Connect marketplace', () => {
  test('register, manager verifies, profiles, campaign, deal, payment, search, chat, inbox', async ({ browser }) => {
    const manager = await browser.newPage()
    const brandPage = await browser.newPage()
    const infPage = await browser.newPage()

    await register(brandPage, brand, 'BRAND')
    await expect(brandPage.getByText('PENDING', { exact: true })).toBeVisible()
    await register(infPage, inf, 'INFLUENCER')

    await login(manager, 'manager@adconnect.local', 'Manager@123')
    await manager.goto('/manager')
    await manager.getByTestId('user-search').fill(brand.email)
    await manager.getByRole('button', { name: 'Find' }).click()
    await expect(manager.getByTestId('user-table')).toContainText(brand.email)
    await manager.locator('tr', { hasText: brand.email }).getByRole('button', { name: 'Verify' }).click()
    await manager.getByTestId('user-search').fill(inf.email)
    await manager.getByRole('button', { name: 'Find' }).click()
    await expect(manager.getByTestId('user-table')).toContainText(inf.email)
    await manager.locator('tr', { hasText: inf.email }).getByRole('button', { name: 'Verify' }).click()

    await brandPage.reload()
    await expect(brandPage.getByText(/VERIFIED/)).toBeVisible({ timeout: 20_000 })
    await brandPage.goto('/profile')
    await brandPage.getByLabel('Company').fill('E2E Corp')
    await brandPage.getByLabel('City').fill('Mumbai')
    await brandPage.getByRole('button', { name: 'Save brand profile' }).click()
    await expect(brandPage.getByText('Brand profile saved')).toBeVisible()

    await brandPage.goto('/campaigns')
    await brandPage.getByLabel('Title').fill(campaignTitle)
    await brandPage.getByLabel('Description').fill('Need fashion creators for a summer launch')
    await brandPage.getByRole('button', { name: 'Publish' }).click()
    await expect(brandPage.getByTestId('campaign-card').filter({ hasText: campaignTitle })).toBeVisible()

    await infPage.reload()
    await infPage.goto('/profile')
    await infPage.getByLabel('Display name').fill(creatorName)
    await infPage.getByLabel('City').fill('Kanpur')
    await infPage.getByLabel(/Niches/).fill('fashion,lifestyle')
    await infPage.getByLabel('Instagram followers').fill('25000')
    await infPage.getByRole('button', { name: 'Save influencer profile' }).click()
    await expect(infPage.getByText('Influencer profile saved')).toBeVisible()

    await infPage.goto('/campaigns')
    await infPage.getByTestId('campaign-card').filter({ hasText: campaignTitle }).click()
    await infPage.getByTestId('apply-form').getByRole('button', { name: 'Submit application' }).click()
    await expect(infPage.getByTestId('deal-status')).toHaveText('APPLIED')

    await brandPage.goto('/deals')
    await brandPage.getByRole('link', { name: campaignTitle }).click()
    await expect(brandPage.getByTestId('deal-status')).toBeVisible()
    await brandPage.getByRole('button', { name: 'Accept' }).click()
    await expect(brandPage.getByTestId('deal-status')).toHaveText('ACCEPTED')
    await brandPage.getByTestId('pay-btn').click()
    await expect(brandPage.getByTestId('deal-status')).toHaveText('FUNDED')

    await infPage.goto('/discover')
    await infPage.getByPlaceholder('Search (fuzzy)').fill(`${creatorName} fashion`)
    await infPage.getByRole('button', { name: 'Search' }).click()
    await expect(infPage.getByTestId('influencer-card').filter({ hasText: creatorName })).toBeVisible({ timeout: 30_000 })

    await brandPage.goto('/deals')
    await brandPage.getByRole('link', { name: campaignTitle }).click()
    await brandPage.getByRole('button', { name: 'Open chat' }).click()
    await expect(brandPage.getByTestId('chat-with')).not.toHaveText('Chat')
    await brandPage.getByTestId('chat-input').fill('Hello from brand')
    await brandPage.getByRole('button', { name: 'Send' }).click()
    await expect(brandPage.getByTestId('chat-log')).toContainText('Hello from brand')

    await infPage.goto('/notifications')
    await expect(infPage.locator('.card').first()).toBeVisible()

    await manager.close()
    await brandPage.close()
    await infPage.close()
  })

  test('gateway rejects unauthenticated deals and public search still works', async ({ request, page }) => {
    const res = await request.get('http://localhost:8080/api/deals/mine')
    expect(res.status()).toBe(401)
    const search = await request.get('http://localhost:8080/api/search/campaigns?q=summer')
    expect(search.ok()).toBeTruthy()
    await page.goto('/login')
    await expect(page.getByTestId('login-form')).toBeVisible()
  })
})
