import { test, expect } from '@playwright/test';
import { POManager } from '../../pages/Common_Files/POManager.js';
import { JiraWebhookTestCaseDesc } from '../../TestCases/JiraWebhookTestCaseDesc.js';

test.describe('webhook-history', () => {
  let poManager;

  test.beforeEach(async ({ page }) => {
    poManager = new POManager(page);
    await poManager.loginPage.pageUrlAsync();
    await poManager.loginPage.loginAsync();
    await expect(poManager.basePage.ui.userProfile.welcomeText).toBeVisible();

    await poManager.basePage.ui.mainNavigation.outboundMenu.click();
    await poManager.basePage.ui.mainNavigation.jiraWebhookMenu.click();
    await expect(poManager.basePage.ui.buttons.gridView).toBeVisible();
  });

  test(JiraWebhookTestCaseDesc.webhookHistoryTestCase, async ({ page }) => {
    const webhookViewPage = poManager.jiraWebhookViewPage;
    const managementPage = poManager.jiraWebhookManagementPage;

    // Step 1: Open the first available webhook
    const firstWebhookCard = page.locator('.webhook-card').first();
    await expect(firstWebhookCard).toBeVisible();

    await expect(async () => {
      await firstWebhookCard.click();
      await expect(webhookViewPage.breadcrumbText).toBeVisible();
    }).toPass({ timeout: 20000, intervals: [1000, 2000, 3000] });

    // Step 2: Verify webhook details page is visible
    await expect(webhookViewPage.webhookDetailsTab).toBeVisible();

    // Step 3: Test webhook and validate history
    await managementPage.validateWebhookTestAndHistory(
      { status: 'SUCCESS' },
      'SUCCESS'
    );

    await webhookViewPage.navigateBackToList();
  });
});