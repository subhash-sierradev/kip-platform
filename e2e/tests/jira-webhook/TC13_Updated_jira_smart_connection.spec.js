import { test, expect } from '@playwright/test';
import { POManager } from '../../pages/Common_Files/POManager.js';
import { JiraWebhookTestCaseDesc } from '../../TestCases/JiraWebhookTestCaseDesc.js';
import { GenerateTestData } from '../../utils/GenerateTestData.js';

test.describe('Jira Webhook Creation - Connect Jira Step (Smart Connection)', () => {
  let poManager;
  let testData;

  test.beforeEach(async ({ page }) => {
    testData = new GenerateTestData().getBasicDetailsTestData();

    poManager = new POManager(page);
    await poManager.loginPage.pageUrlAsync();
    await poManager.loginPage.loginAsync();
  });

  // Smart connection manager — uses existing connection if available,
  // otherwise auto-creates a new one using credentials from .env
  // jiraConnectionName is dynamically generated: JiraConn_<word>_<timestamp>
  test(JiraWebhookTestCaseDesc.connectJiraSmartTestCase, async ({ page }) => {
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

    const jsonPayload = testData.samplePayload;
    await page.getByRole('textbox', { name: 'Paste your JSON payload here' }).fill(JSON.stringify(jsonPayload));
    await connectJiraPage.ui.buttons.next.click();

    // Verify Connection Method step is visible
    await expect(page.getByText('Connection Method')).toBeVisible();

    // Smart orchestrator: tries existing connection first, falls back to new
    const mode = await connectJiraPage.manageJiraConnectionUpdated(testData.jiraConnectionName);

    expect(['existing', 'new']).toContain(mode);

    // Verify Next button is enabled and proceed to Field Mapping
    await expect(connectJiraPage.ui.buttons.next).toBeEnabled();
    await connectJiraPage.clickNext();
    await expect(page.getByRole('heading', { name: 'Incoming JSON' })).toBeVisible();
  });
});
