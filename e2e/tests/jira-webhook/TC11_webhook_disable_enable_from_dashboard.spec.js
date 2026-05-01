// spec: Based on Kaseware Integration Platform Test Plan - Jira Webhook Actions and Management
// seed: tests/seed.spec.ts

import { test, expect } from '@playwright/test';
import { POManager } from '../../pages/Common_Files/POManager.js';
import { JiraWebhookTestCaseDesc } from '../../TestCases/JiraWebhookTestCaseDesc.js';
import { GenerateTestData } from '../../utils/GenerateTestData.js';

test.describe('Jira Webhook Management', () => {
  let poManager;
  let testData;

  test.beforeEach(async ({ page }) => {
    testData = new GenerateTestData().getBasicDetailsTestData();

    poManager = new POManager(page);
    await poManager.loginPage.pageUrlAsync();
    await poManager.loginPage.loginAsync();
    await expect(poManager.basePage.ui.userProfile.welcomeText).toBeVisible({ timeout: 15000 });
  });

  test(JiraWebhookTestCaseDesc.disableWebhookFromDashboardTestCase, async ({ page }) => {
    const webhookManagementPage = poManager.jiraWebhookManagementPage;

    // Create webhook
    await poManager.createJiraWebhookPage.createJiraWebhook(testData);
    await expect(poManager.basePage.ui.buttons.gridView).toBeVisible();

    await webhookManagementPage.searchWebhook(testData.validWebhookName);
    await expect(webhookManagementPage.getWebhookText(testData.validWebhookName)).toBeVisible();

    // Disable the webhook
    await webhookManagementPage.disableWebhook(testData.validWebhookName);
    await expect(webhookManagementPage.disableDialog).toBeVisible();
    await expect(webhookManagementPage.disableWarningMessage).toBeVisible();
    await webhookManagementPage.confirmDisable();
    await expect(webhookManagementPage.disableSuccessNotification).toBeVisible({ timeout: 10000 });

    await webhookManagementPage.clearAndSearchWebhook(testData.validWebhookName);
    await expect(webhookManagementPage.getWebhookText(testData.validWebhookName)).toBeVisible({ timeout: 5000 });
    await expect(webhookManagementPage.getWebhookStatusBadge(testData.validWebhookName, 'Disabled')).toBeVisible();

    // Enable the webhook
    await webhookManagementPage.enableWebhook(testData.validWebhookName);
    await expect(webhookManagementPage.enableDialog).toBeVisible();
    await expect(webhookManagementPage.enableWarningMessage).toBeVisible();
    await webhookManagementPage.confirmEnable();
    await expect(webhookManagementPage.enableSuccessNotification).toBeVisible({ timeout: 10000 });

    await webhookManagementPage.clearAndSearchWebhook(testData.validWebhookName);
    await expect(webhookManagementPage.getWebhookText(testData.validWebhookName)).toBeVisible({ timeout: 5000 });
    await expect(webhookManagementPage.getWebhookStatusBadge(testData.validWebhookName, 'Enabled')).toBeVisible();
  });
});
