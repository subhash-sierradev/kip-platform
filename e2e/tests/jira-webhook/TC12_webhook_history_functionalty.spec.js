import { test, expect } from '@playwright/test';
import { POManager } from '../../pages/Common_Files/POManager.js';
import { JiraWebhookViewPage } from '../../pages/Jira_Webhook/JiraWebhookViewPage.js';
import { JiraWebhookTestCaseDesc } from '../../TestCases/JiraWebhookTestCaseDesc.js';
import { GenerateTestData } from '../../utils/GenerateTestData.js';

test.describe('webhook-history', () => 
{
  let testDataGenerator;
  let testData;
  let createdWebhookName;
  let poManager;

  test.beforeEach(async ({ page }) => 
  {
    testDataGenerator = new GenerateTestData();
    testData = testDataGenerator.getBasicDetailsTestData();
    createdWebhookName = testData.validWebhookName;
    
    poManager = new POManager(page);
    await poManager.loginPage.pageUrlAsync();
    await poManager.loginPage.loginAsync();
    await expect(poManager.basePage.ui.userProfile.welcomeText).toBeVisible();
    
    await poManager.createJiraWebhookPage.createJiraWebhook(testData);
    await expect(poManager.basePage.ui.buttons.gridView).toBeVisible();
  });

  test(JiraWebhookTestCaseDesc.webhookHistoryTestCase, async ({ page }) => 
  {
    const webhookManagementPage = poManager.jiraWebhookManagementPage;
    const webhookViewPage = new JiraWebhookViewPage(page);
    
    // Step 1: Open the created webhook
    await webhookManagementPage.searchWebhook(createdWebhookName);
    
    const webhookCard = webhookManagementPage.getWebhookCard(createdWebhookName);
    
    // Click and wait for navigation with retry - if navigation fails, retry the click
    await expect(async () => {
      await webhookCard.click();
      await expect(webhookViewPage.breadcrumbText).toBeVisible();
    }).toPass({ timeout: 20000, intervals: [1000, 2000, 3000] });
    
    // Step 2: Verify webhook details page is visible
    await expect(webhookViewPage.webhookDetailsTab).toBeVisible();
    
    // Step 3: Perform complete webhook test and history validation
    await webhookManagementPage.validateWebhookTestAndHistory(
      { status: 'SUCCESS' },
      'SUCCESS'
    );
    
    // Navigate back to list for cleanup
    await webhookViewPage.navigateBackToList();
  });
});