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

  test(JiraWebhookTestCaseDesc.testDisabledWebhookTestCase, async ({ page }) => {
    const webhookManagementPage = poManager.jiraWebhookManagementPage;
    const webhookViewPage = poManager.jiraWebhookViewPage;

    // STEP 1: Create a new Jira Webhook
    await poManager.createJiraWebhookPage.createJiraWebhook(testData);
    await expect(poManager.basePage.ui.buttons.gridView).toBeVisible();

    // STEP 2: Search for the created webhook by name
    await webhookManagementPage.searchWebhook(testData.validWebhookName);
    await expect(webhookManagementPage.getWebhookText(testData.validWebhookName)).toBeVisible();

    // STEP 3: Navigate to webhook details page first (TC12 pattern)
    const webhookCard = webhookManagementPage.getWebhookCard(testData.validWebhookName);
    await expect(webhookCard).toBeVisible();

    await expect(async () => {
      await webhookCard.click();
      await expect(webhookViewPage.breadcrumbText).toBeVisible();
    }).toPass({ timeout: 20000, intervals: [1000, 2000, 3000] });

    await expect(webhookViewPage.webhookDetailsTab).toBeVisible();

    // STEP 4: Disable the webhook from details page
    await expect(webhookViewPage.disableWebhookButton).toBeVisible({ timeout: 10000 });
    await webhookViewPage.disableWebhookButton.click();

    // Confirm disable action in dialog
    await expect(webhookManagementPage.disableDialog).toBeVisible();
    await expect(webhookManagementPage.disableWarningMessage).toBeVisible();
    await webhookManagementPage.confirmDisable();
    await expect(webhookManagementPage.disableSuccessNotification).toBeVisible({ timeout: 10000 });

    // STEP 5: Verify Test Webhook button is DISABLED on details page
    await expect(webhookViewPage.testWebhookButton).toBeVisible({ timeout: 10000 });
    await expect(webhookViewPage.testWebhookButton).toBeDisabled();
    await expect(webhookViewPage.enableWebhookButton).toBeVisible({ timeout: 10000 });
    await expect(webhookViewPage.disableWebhookButton).not.toBeVisible();

    // STEP 6: Enable the webhook from the details page
    await expect(webhookViewPage.enableWebhookButton).toBeVisible({ timeout: 10000 });
    await webhookViewPage.enableWebhookButton.click();

    // Confirm enable action in dialog
    await expect(webhookManagementPage.enableDialog).toBeVisible();
    await expect(webhookManagementPage.enableWarningMessage).toBeVisible();
    await webhookManagementPage.confirmEnable();
    await expect(webhookManagementPage.enableSuccessNotification).toBeVisible({ timeout: 10000 });

    // STEP 7: Verify Test Webhook button is ENABLED after enabling the webhook
    await expect(webhookViewPage.testWebhookButton).toBeVisible({ timeout: 10000 });
    await expect(webhookViewPage.testWebhookButton).toBeEnabled();
    await expect(webhookViewPage.disableWebhookButton).toBeVisible({ timeout: 10000 });
    await expect(webhookViewPage.enableWebhookButton).not.toBeVisible();
  });
});
