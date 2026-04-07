// spec: Based on Kaseware Integration Platform Test Plan - Jira Webhook Actions and Management
// seed: tests/seed.spec.ts

import { test, expect } from '@playwright/test';
import { POManager } from '../../pages/Common_Files/POManager.js';
import { JiraWebhookCreatorPage } from '../../pages/Jira_Webhook/CreateJiraWebhookPage.js';
import { JiraWebhookManagementPage } from '../../pages/Jira_Webhook/JiraWebhookManagementPage.js';
import { JiraWebhookTestCaseDesc } from '../../TestCases/JiraWebhookTestCaseDesc.js';
import { GenerateTestData } from '../../utils/GenerateTestData.js';

test.describe('Jira Webhook Management', () => {
  let testDataGenerator;
  let testData;
  let createdWebhookName;
  let clonedWebhookName;
  let jiraWebhookCreator;

  test.beforeEach(async ({ page }) => {
    // Initialize test data generator
    testDataGenerator = new GenerateTestData();
    testData = testDataGenerator.getBasicDetailsTestData();
    createdWebhookName = testData.validWebhookName;
    
    // Initialize page objects
    jiraWebhookCreator = new JiraWebhookCreatorPage(page);
    
    const poManager = new POManager(page);
    await poManager.loginPage.pageUrlAsync();
    await poManager.loginPage.loginAsync();
    // Wait for page to be ready - increased timeout for repeated test runs
    await expect(poManager.basePage.ui.userProfile.welcomeText).toBeVisible({ timeout: 15000 });
  });

  test(JiraWebhookTestCaseDesc.cloneWebhookFromDashboardTestCase, async ({ page }) => {
    // Initialize page objects
    const webhookManagementPage = new JiraWebhookManagementPage(page);
    const poManager = new POManager(page);
    // STEP 1: CREATE WEBHOOK FIRST
    await jiraWebhookCreator.createJiraWebhook(testData);

    // STEP 2: NOW CLONE THE CREATED WEBHOOK
    // Verify we're back on the webhook list page in Grid View
    await expect(poManager.basePage.ui.buttons.gridView).toBeVisible();
    
    // Search for the created webhook by name and verify it's visible
    await webhookManagementPage.searchWebhook(createdWebhookName);
    await expect(webhookManagementPage.getWebhookText(createdWebhookName)).toBeVisible();

    // Clone the webhook
    await webhookManagementPage.cloneWebhook(createdWebhookName);
    
    // Verify clone dialog opens and "Copy of" is automatically prepended to webhook name
    clonedWebhookName = await webhookManagementPage.verifyCloneDialogAndGetClonedName(createdWebhookName);
    
    // Verify the expected cloned name format is "Copy of [originalName]"
    expect(clonedWebhookName).toBe(`Copy of ${createdWebhookName}`);
    
    // Proceed with clone wizard (keeps the "Copy of" prefixed name)
    await webhookManagementPage.proceedWithCloneWizard();
    
    // Complete the full clone wizard flow (all data is pre-filled from original webhook)
    await webhookManagementPage.completeCloneWizard();
    
    // Verify clone success and return to webhook list
    await webhookManagementPage.verifyCloneSuccessAndReturn();

    // STEP 3: VERIFY BOTH WEBHOOKS EXIST AFTER SUCCESSFUL CLONE
    // Clear search to show all webhooks
    await webhookManagementPage.clearAndSearchWebhook('');
    
    // Verify both original and cloned webhooks exist in the list
    await expect(webhookManagementPage.getWebhookText(createdWebhookName)).toBeVisible();
    await expect(webhookManagementPage.getWebhookText(clonedWebhookName)).toBeVisible();
    
    // Search for cloned webhook to verify it was created successfully
    await webhookManagementPage.clearAndSearchWebhook(clonedWebhookName);
    await expect(webhookManagementPage.getWebhookText(clonedWebhookName)).toBeVisible();
  });
});