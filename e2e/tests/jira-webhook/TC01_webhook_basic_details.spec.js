import { test, expect } from '@playwright/test';
import { POManager } from '../../pages/Common_Files/POManager.js';
import { JiraWebhookBasicDetailsPage } from '../../pages/Jira_Webhook/JiraWebhookBasicDetailsPage.js';
import { JiraWebhookTestCaseDesc } from '../../TestCases/JiraWebhookTestCaseDesc.js';
import { GenerateTestData } from '../../utils/GenerateTestData.js';

test.describe('Jira Webhook Creation - Navigation and Basic Details', () => {
  let testDataGenerator;
  let testData;
  let basicDetailsPage;

  test.beforeEach(async ({ page }) => {
    // Initialize test data generator
    testDataGenerator = new GenerateTestData();
    testData = testDataGenerator.getBasicDetailsTestData();
    
    // Initialize page objects
    basicDetailsPage = new JiraWebhookBasicDetailsPage(page);
    
    const poManager = new POManager(page);
    await poManager.loginPage.pageUrlAsync();
    await poManager.loginPage.loginAsync();
  });

  test(JiraWebhookTestCaseDesc.basicDetailsTestCase, async ({ page }) => {
    // Login verification
    await expect(basicDetailsPage.ui.userProfile.welcomeText).toBeVisible();

    // Navigate to webhook creation
    await basicDetailsPage.navigateToJiraWebhook();
    await expect(basicDetailsPage.ui.buttons.gridView).toBeVisible();

    // Start webhook creation wizard
    await basicDetailsPage.ui.buttons.addJiraWebhook.click();
    await expect(basicDetailsPage.ui.headings.createJiraWebhook).toBeVisible();
    
    // Verify Next button is initially disabled (required field validation)
    await expect(basicDetailsPage.ui.buttons.next).toBeDisabled();

    // Fill required webhook name and verify button enables
    await basicDetailsPage.fillWebhookName(testData.validWebhookName);
    await expect(basicDetailsPage.ui.buttons.next).toBeEnabled();

    // Fill optional description
    await basicDetailsPage.fillDescription(testData.validDescription);

    // Proceed to next step
    await basicDetailsPage.clickNext();
    await expect(page.getByRole('heading', { name: 'Sample Webhook Payload for Jira Field Mapping' })).toBeVisible();
  });
});