// spec: Based on Kaseware Integration Platform Test Plan - Jira Webhook Actions and Management
// seed: tests/seed.spec.ts

import { test, expect } from '@playwright/test';
import { POManager } from '../../pages/Common_Files/POManager.js';
import { JiraWebhookTestCaseDesc } from '../../TestCases/JiraWebhookTestCaseDesc.js';
import { GenerateTestData } from '../../utils/GenerateTestData.js';

test.describe('Jira Webhook Management', () => {
  let testDataGenerator;
  let testData;
  let createdWebhookName;
  let poManager;

  test.beforeEach(async ({ page }) => {
    // Initialize test data generator
    testDataGenerator = new GenerateTestData();
    testData = testDataGenerator.getBasicDetailsTestData();
    createdWebhookName = testData.validWebhookName;
    
    // Initialize POManager
    poManager = new POManager(page);
    await poManager.loginPage.pageUrlAsync();
    await poManager.loginPage.loginAsync();
    await expect(poManager.basePage.ui.userProfile.welcomeText).toBeVisible({ timeout: 15000 });
  });

  test(JiraWebhookTestCaseDesc.disableWebhookFromDashboardTestCase, async ({ page }) => {
    // Initialize page objects from POManager
    const webhookManagementPage = poManager.jiraWebhookManagementPage;
    
    // Create webhook
    await poManager.createJiraWebhookPage.createJiraWebhook(testData);
    
    // Wait for webhook creation to complete and page to stabilize
    await page.waitForLoadState('domcontentloaded');
    await expect(poManager.basePage.ui.buttons.gridView).toBeVisible();
    await webhookManagementPage.searchWebhook(createdWebhookName);
    await expect(webhookManagementPage.getWebhookText(createdWebhookName)).toBeVisible();
    // Disable the webhook
    await webhookManagementPage.disableWebhook(createdWebhookName);
    await expect(webhookManagementPage.disableDialog).toBeVisible();
    await expect(webhookManagementPage.disableWarningMessage).toBeVisible();
    await webhookManagementPage.confirmDisable();
    await expect(webhookManagementPage.disableSuccessNotification).toBeVisible({ timeout: 10000 });
    await webhookManagementPage.clearAndSearchWebhook(createdWebhookName);
    await expect(webhookManagementPage.getWebhookText(createdWebhookName)).toBeVisible({ timeout: 5000 });
    
    // Verify webhook status changed to Disabled
    const disabledStatusBadge = webhookManagementPage.getWebhookStatusBadge(createdWebhookName, 'Disabled');
    await expect(disabledStatusBadge).toBeVisible();
    // Enable the webhook
    await webhookManagementPage.enableWebhook(createdWebhookName);
    await expect(webhookManagementPage.enableDialog).toBeVisible();
    await expect(webhookManagementPage.enableWarningMessage).toBeVisible();
    await webhookManagementPage.confirmEnable();
    await expect(webhookManagementPage.enableSuccessNotification).toBeVisible({ timeout: 10000 });
    await webhookManagementPage.clearAndSearchWebhook(createdWebhookName);
    await expect(webhookManagementPage.getWebhookText(createdWebhookName)).toBeVisible({ timeout: 5000 });
    
    // Verify webhook status changed to Enabled
    const enabledStatusBadge = webhookManagementPage.getWebhookStatusBadge(createdWebhookName, 'Enabled');
    await expect(enabledStatusBadge).toBeVisible();
  });
});
