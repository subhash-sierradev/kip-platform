import { test, expect } from '@playwright/test';
import { POManager } from '../../pages/Common_Files/POManager.js';
import { JiraWebhookTestCaseDesc } from '../../TestCases/JiraWebhookTestCaseDesc.js';
import { GenerateTestData } from '../../utils/GenerateTestData.js';

test.describe('Jira Webhook Creation - Connect Jira Step', () => {
  let poManager;
  let testData;

  test.beforeEach(async ({ page }) => {
    testData = new GenerateTestData().getBasicDetailsTestData();

    poManager = new POManager(page);
    await poManager.loginPage.pageUrlAsync();
    await poManager.loginPage.loginAsync();
  });

  test(JiraWebhookTestCaseDesc.connectJiraTestCase, async ({ page }) => {
    const connectJiraPage = poManager.jiraWebhookConnectJiraPage;
    const nav = poManager.ui.mainNavigation;

    // Navigate and complete previous steps
    await nav.outboundMenu.click();
    await nav.jiraWebhookMenu.click();
    await poManager.ui.buttons.addJiraWebhook.click();
    await expect(page.getByRole('heading', { name: 'Create Jira Webhook' })).toBeVisible();

    const nameInput = page.getByRole('textbox', { name: 'Jira Webhook Name' });
    await nameInput.click();
    await nameInput.fill(testData.validWebhookName);
    await connectJiraPage.ui.buttons.next.click();

    await page.getByRole('textbox', { name: 'Paste your JSON payload here' }).fill(JSON.stringify(testData.samplePayload));
    await connectJiraPage.ui.buttons.next.click();

    // Verify Connection Method step and default selection
    await expect(page.getByText('Connection Method')).toBeVisible();
    await expect(connectJiraPage.ui.formElements.useExistingConnection).toBeChecked();
    await expect(connectJiraPage.ui.buttons.next).toBeDisabled();

    // Select and verify connection
    try {
      await connectJiraPage.selectConnection();
      await connectJiraPage.waitForVerifyButtonEnabled();
      await connectJiraPage.verifyConnection();
      await expect(page.getByRole('button', { name: 'Verified' })).toBeVisible({ timeout: 15000 });
    } catch (error) {
      const connectionCountText = await page.getByText(/connections.*active.*failed/).textContent({ timeout: 3000 }).catch(() => null);
      if (connectionCountText?.includes('0 active')) {
        test.skip();
      }
      throw error;
    }

    // Verify Next button enables and proceed
    await expect(connectJiraPage.ui.buttons.next).toBeEnabled();
    await connectJiraPage.clickNext();
    await expect(page.getByRole('heading', { name: 'Incoming JSON' })).toBeVisible();
  });
});