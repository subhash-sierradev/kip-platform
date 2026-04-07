import { test, expect } from '@playwright/test';
import { POManager } from '../../pages/Common_Files/POManager.js';
import { JiraWebhookConnectJiraPage } from '../../pages/Jira_Webhook/JiraWebhookConnectJiraPage.js';
import { JiraWebhookTestCaseDesc } from '../../TestCases/JiraWebhookTestCaseDesc.js';
import { GenerateTestData } from '../../utils/GenerateTestData.js';

// Helper function to navigate to webhook creation
const navigateToWebhookCreation = async (page) => {
  const ui = page.ui || (new (await import('../../pages/Common_Files/UICommonElements.js')).UICommonElements(page));
  await ui.mainNavigation.outboundMenu.click();
  await ui.mainNavigation.jiraWebhookMenu.click();
  await ui.buttons.addJiraWebhook.click();
};

test.describe('Jira Webhook Creation - Connect Jira Step', () => {
  let testDataGenerator;
  let testData;

  test.beforeEach(async ({ page }) => {
    // Initialize test data generator
    testDataGenerator = new GenerateTestData();
    testData = testDataGenerator.getBasicDetailsTestData();
    
    const poManager = new POManager(page);
    await poManager.loginPage.pageUrlAsync();
    await poManager.loginPage.loginAsync();
  });

  test(JiraWebhookTestCaseDesc.connectJiraTestCase, async ({ page }) => {
    const connectJiraPage = new JiraWebhookConnectJiraPage(page);
    
    // Navigate and complete previous steps
    await navigateToWebhookCreation(page);
    await page.getByRole('textbox', { name: 'Jira Webhook Name' }).pressSequentially(testData.validWebhookName, { delay: 50 });
    await connectJiraPage.ui.buttons.next.click();
    
    const jsonPayload = testData.samplePayload;
    await page.getByRole('textbox', { name: 'Paste your JSON payload here' }).fill(JSON.stringify(jsonPayload));
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
      // Skip if no active connections
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