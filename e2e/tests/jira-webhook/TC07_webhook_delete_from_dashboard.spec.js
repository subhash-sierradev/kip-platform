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

  test(JiraWebhookTestCaseDesc.deleteWebhookFromDashboardTestCase, async ({ page }) => {
    // Initialize page objects
    const webhookManagementPage = new JiraWebhookManagementPage(page);
    const poManager = new POManager(page);

    // STEP 1: CREATE WEBHOOK FIRST
    await jiraWebhookCreator.createJiraWebhook(testData);
    
    // Wait for return to dashboard - verify grid view is loaded
    await expect(poManager.basePage.ui.buttons.gridView).toBeVisible();

    // STEP 2: NOW SEARCH AND DELETE THE CREATED WEBHOOK
    // Search for the created webhook by name and verify it's visible
    await webhookManagementPage.searchWebhook(createdWebhookName);
    await expect(webhookManagementPage.getWebhookText(createdWebhookName)).toBeVisible();

    // Delete the webhook
    await webhookManagementPage.deleteWebhook(createdWebhookName);
    
    // Verify the delete confirmation dialog is displayed
    await expect(webhookManagementPage.deleteDialog).toBeVisible();

    // Verify the warning message about permanent deletion is displayed
    await expect(webhookManagementPage.deleteWarningMessage).toBeVisible();

    // Confirm webhook deletion
    await webhookManagementPage.confirmDeletion();

    // Verify successful deletion by checking for success notification
    await expect(webhookManagementPage.successNotification).toBeVisible();
    
    // Search for the deleted webhook to verify it no longer exists
    await webhookManagementPage.clearAndSearchWebhook(createdWebhookName);
    // Wait for search to process and verify the deleted webhook is not found
    await expect(webhookManagementPage.getWebhookText(createdWebhookName)).not.toBeVisible();
  });
});
