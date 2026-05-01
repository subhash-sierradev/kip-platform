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

  test(JiraWebhookTestCaseDesc.deleteWebhookFromDashboardTestCase, async ({ page }) => {
    const webhookManagementPage = poManager.jiraWebhookManagementPage;

    // STEP 1: CREATE WEBHOOK FIRST
    await poManager.createJiraWebhookPage.createJiraWebhook(testData);
    await expect(poManager.basePage.ui.buttons.gridView).toBeVisible();

    // STEP 2: SEARCH, DELETE AND VERIFY
    await webhookManagementPage.searchWebhook(testData.validWebhookName);
    await expect(webhookManagementPage.getWebhookText(testData.validWebhookName)).toBeVisible();

    await webhookManagementPage.deleteWebhook(testData.validWebhookName);
    await expect(webhookManagementPage.deleteDialog).toBeVisible();
    await expect(webhookManagementPage.deleteWarningMessage).toBeVisible();

    await webhookManagementPage.confirmDeletion();
    await expect(webhookManagementPage.successNotification).toBeVisible();

    await webhookManagementPage.clearAndSearchWebhook(testData.validWebhookName);
    await expect(webhookManagementPage.getWebhookText(testData.validWebhookName)).not.toBeVisible();
  });
});
