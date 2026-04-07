import { test, expect } from '@playwright/test';

test.describe('Test group', () => {
  test('seed', async ({ page }) => {
    await page.goto('https://kaseware.sierradev.com');
    await expect(page.getByRole('heading', { name: 'Sign in to your account' })).toBeVisible();
    await page.getByRole('textbox', { name: 'Username or email' }).fill('org2-admin');
    await page.getByRole('textbox', { name: 'Password' }).fill('Abc@12345');
    await page.getByRole('button', { name: 'Sign In' }).click();
  });
});
