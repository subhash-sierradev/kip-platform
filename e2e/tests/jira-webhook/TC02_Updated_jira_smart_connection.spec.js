import { test, expect } from '@playwright/test';
import { POManager } from '../../pages/Common_Files/POManager.js';
import { JiraWebhookConnectJiraPage } from '../../pages/Jira_Webhook/JiraWebhookConnectJiraPage.js';
import { JiraWebhookTestCaseDesc } from '../../TestCases/JiraWebhookTestCaseDesc.js';
import { GenerateTestData } from '../../utils/GenerateTestData.js';

// Helper function to navigate to webhook creation
const navigateToWebhookCreation = async (page) => {
  const { UICommonElements } = await import('../../pages/Common_Files/UICommonElements.js');
  const ui = new UICommonElements(page);
  await ui.mainNavigation.outboundMenu.click();
  await ui.mainNavigation.jiraWebhookMenu.click();
  await ui.buttons.addJiraWebhook.click();
};

test.describe('Jira Webhook Creation - Connect Jira Step (Smart Connection)', () => {
  let testDataGenerator;
  let testData;

  test.beforeEach(async ({ page }) => {
    testDataGenerator = new GenerateTestData();
    testData = testDataGenerator.getBasicDetailsTestData();

    const poManager = new POManager(page);
    await poManager.loginPage.pageUrlAsync();
    await poManager.loginPage.loginAsync();
  });

  // Smart connection manager — uses existing connection if available,
  // otherwise auto-creates a new one using credentials from .env
  // jiraConnectionName is dynamically generated: JiraConn_<word>_<timestamp>
  test(JiraWebhookTestCaseDesc.connectJiraSmartTestCase, async ({ page }) => {
    const connectJiraPage = new JiraWebhookConnectJiraPage(page);

    // Navigate and complete previous steps
    await navigateToWebhookCreation(page);
    await page.getByRole('textbox', { name: 'Jira Webhook Name' }).pressSequentially(testData.validWebhookName, { delay: 50 });
    await connectJiraPage.ui.buttons.next.click();

    const jsonPayload = testData.samplePayload;
    await page.getByRole('textbox', { name: 'Paste your JSON payload here' }).fill(JSON.stringify(jsonPayload));
    await connectJiraPage.ui.buttons.next.click();

    // Verify Connection Method step is visible
    await expect(page.getByText('Connection Method')).toBeVisible();

    // Smart orchestrator: tries existing connection first, falls back to new
    const mode = await connectJiraPage.manageJiraConnectionUpdated(testData.jiraConnectionName);

    // Log which branch was taken (existing | new)
    console.log(`[TC02_Updated] Connection established via: ${mode}`);
    expect(['existing', 'new']).toContain(mode);

    // Verify Next button is enabled and proceed to Field Mapping
    await expect(connectJiraPage.ui.buttons.next).toBeEnabled();
    await connectJiraPage.clickNext();
    await expect(page.getByRole('heading', { name: 'Incoming JSON' })).toBeVisible();
  });
});
