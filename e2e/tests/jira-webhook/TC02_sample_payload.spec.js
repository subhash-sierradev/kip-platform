import { test, expect } from '@playwright/test';
import { POManager } from '../../pages/Common_Files/POManager.js';
import { JiraWebhookTestCaseDesc } from '../../TestCases/JiraWebhookTestCaseDesc.js';
import { GenerateTestData } from '../../utils/GenerateTestData.js';

// Helper function to navigate to webhook creation
const navigateToWebhookCreation = async (page) => {
  const ui = page.ui || (new (await import('../../pages/Common_Files/UICommonElements.js')).UICommonElements(page));
  await ui.mainNavigation.outboundMenu.click();
  await ui.mainNavigation.jiraWebhookMenu.click();
  await ui.buttons.addJiraWebhook.click();
};

test.describe('Jira Webhook Creation - Sample Payload Step', () => {
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

  test(JiraWebhookTestCaseDesc.samplePayloadTestCase, async ({ page }) => {
    const poManager = new POManager(page);
    
    // Navigate to webhook creation and complete basic details
    await navigateToWebhookCreation(page);
    await page.getByRole('textbox', { name: 'Jira Webhook Name' }).pressSequentially(testData.validWebhookName, { delay: 50 });
    await page.getByRole('textbox', { name: 'Provide a brief description' }).fill(testData.validDescription);
    await poManager.basePage.ui.buttons.next.click();

    // Verify Sample Payload step is loaded
    await expect(page.getByRole('heading', { name: 'Sample Webhook Payload for Jira Field Mapping' })).toBeVisible();

    // Verify Format JSON and Next buttons are initially disabled
    await expect(poManager.basePage.ui.buttons.formatJson).toBeDisabled();
    await expect(poManager.basePage.ui.buttons.next).toBeDisabled();

    // Fill JSON payload and verify buttons become enabled
    const jsonPayload = testData.samplePayload;
    await page.getByRole('textbox', { name: 'Paste your JSON payload here' }).fill(JSON.stringify(jsonPayload));
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