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

  test(JiraWebhookTestCaseDesc.cloneWebhookFromDashboardTestCase, async ({ page }) => {
    const webhookManagementPage = poManager.jiraWebhookManagementPage;

    // STEP 1: CREATE WEBHOOK FIRST
    await poManager.createJiraWebhookPage.createJiraWebhook(testData);
    await expect(poManager.basePage.ui.buttons.gridView).toBeVisible();

    // STEP 2: CLONE THE CREATED WEBHOOK
    await webhookManagementPage.searchWebhook(testData.validWebhookName);
    await expect(webhookManagementPage.getWebhookText(testData.validWebhookName)).toBeVisible();

    await webhookManagementPage.cloneWebhook(testData.validWebhookName);

    const clonedWebhookName = await webhookManagementPage.verifyCloneDialogAndGetClonedName(testData.validWebhookName);
    expect(clonedWebhookName).toBe(`Copy of ${testData.validWebhookName}`);

    await webhookManagementPage.proceedWithCloneWizard();
    await webhookManagementPage.completeCloneWizard();
    await webhookManagementPage.verifyCloneSuccessAndReturn();

    // STEP 3: VERIFY BOTH WEBHOOKS EXIST AFTER SUCCESSFUL CLONE
    await webhookManagementPage.clearAndSearchWebhook('');
    await expect(webhookManagementPage.getWebhookText(testData.validWebhookName)).toBeVisible();
    await expect(webhookManagementPage.getWebhookText(clonedWebhookName)).toBeVisible();

    await webhookManagementPage.clearAndSearchWebhook(clonedWebhookName);
    await expect(webhookManagementPage.getWebhookText(clonedWebhookName)).toBeVisible();
  });
});