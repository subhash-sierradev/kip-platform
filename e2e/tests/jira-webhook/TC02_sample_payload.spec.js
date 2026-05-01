import { test, expect } from '@playwright/test';
import { POManager } from '../../pages/Common_Files/POManager.js';
import { JiraWebhookTestCaseDesc } from '../../TestCases/JiraWebhookTestCaseDesc.js';
import { GenerateTestData } from '../../utils/GenerateTestData.js';

test.describe('Jira Webhook Creation - Sample Payload Step', () => {
  let poManager;
  let testData;

  test.beforeEach(async ({ page }) => {
    testData = new GenerateTestData().getBasicDetailsTestData();

    poManager = new POManager(page);
    await poManager.loginPage.pageUrlAsync();
    await poManager.loginPage.loginAsync();
  });

  test(JiraWebhookTestCaseDesc.samplePayloadTestCase, async ({ page }) => {
    const nav = poManager.ui.mainNavigation;

    // Navigate to webhook creation and complete basic details
    await nav.outboundMenu.click();
    await nav.jiraWebhookMenu.click();
    await poManager.ui.buttons.addJiraWebhook.click();
    await expect(page.getByRole('heading', { name: 'Create Jira Webhook' })).toBeVisible();

    const nameInput = page.getByRole('textbox', { name: 'Jira Webhook Name' });
    await nameInput.click();
    await nameInput.fill(testData.validWebhookName);
    await page.getByRole('textbox', { name: 'Provide a brief description' }).fill(testData.validDescription);
    await poManager.basePage.ui.buttons.next.click();

    // Verify Sample Payload step is loaded
    await expect(page.getByRole('heading', { name: 'Sample Webhook Payload for Jira Field Mapping' })).toBeVisible();

    // Verify Format JSON and Next buttons are initially disabled
    await expect(poManager.basePage.ui.buttons.formatJson).toBeDisabled();
    await expect(poManager.basePage.ui.buttons.next).toBeDisabled();

    // Fill JSON payload and verify buttons become enabled
    await page.getByRole('textbox', { name: 'Paste your JSON payload here' }).fill(JSON.stringify(testData.samplePayload));
    await expect(poManager.basePage.ui.buttons.formatJson).toBeEnabled();
    await expect(poManager.basePage.ui.buttons.next).toBeEnabled();

    // Format JSON and verify it works
    await poManager.basePage.ui.buttons.formatJson.click();
    await expect(page.getByRole('textbox', { name: 'Paste your JSON payload here' })).toHaveValue(/"form"/);

    // Proceed to next step
    await poManager.basePage.ui.buttons.next.click();
    await expect(page.getByText('Connection Method')).toBeVisible();
  });
});