// spec: Based on Kaseware Integration Platform Test Plan - Jira Webhook Data Verification
// seed: tests/seed.spec.ts

import { test, expect } from '@playwright/test';
import { POManager } from '../../pages/Common_Files/POManager.js';
import { JiraWebhookCreatorPage } from '../../pages/Jira_Webhook/CreateJiraWebhookPage.js';
import { JiraWebhookConnectJiraPage } from '../../pages/Jira_Webhook/JiraWebhookConnectJiraPage.js';
import { JiraWebhookFieldMappingPage } from '../../pages/Jira_Webhook/JiraWebhookFieldMappingPage.js';
import { JiraWebhookManagementPage } from '../../pages/Jira_Webhook/JiraWebhookManagementPage.js';
import { JiraWebhookViewPage } from '../../pages/Jira_Webhook/JiraWebhookViewPage.js';
import { JiraWebhookTestCaseDesc } from '../../TestCases/JiraWebhookTestCaseDesc.js';
import { GenerateTestData } from '../../utils/GenerateTestData.js';

test.describe('Jira Webhook Data Verification', () => {
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
    await expect(poManager.basePage.ui.userProfile.welcomeText).toBeVisible({ timeout: 15000 });
  });

  test(JiraWebhookTestCaseDesc.viewWebhookDetailsPageVerificationTestCase, async ({ page }) => {
    // Initialize page objects
    const connectJiraPage = new JiraWebhookConnectJiraPage(page);
    const fieldMappingPage = new JiraWebhookFieldMappingPage(page);
    const webhookManagementPage = new JiraWebhookManagementPage(page);
    const webhookViewPage = new JiraWebhookViewPage(page);
    
    // Wait for successful login and home page to load
    const poManager = new POManager(page);
   

    // STEP 1: CREATE WEBHOOK WITH DYNAMIC TEST DATA
    await jiraWebhookCreator.createJiraWebhook(testData);

    // STEP 2: NAVIGATE TO WEBHOOK VIEW PAGE AND VERIFY DATA
    // Search for the created webhook
    await webhookManagementPage.searchWebhook(createdWebhookName);
    
    // Wait for webhook NAME text to be visible (confirms search worked)
    await expect(webhookManagementPage.getWebhookText(createdWebhookName)).toBeVisible({ timeout: 10000 });
    
    // Get the webhook CARD (not just text) and wait for it to be ready
    const webhookCard = webhookManagementPage.getWebhookCard(createdWebhookName);
    await expect(webhookCard).toBeVisible({ timeout: 10000 });
    
    // Click and wait for navigation with retry - if navigation fails, retry the click
    await expect(async () => {
      await webhookCard.click();
      await expect(webhookViewPage.breadcrumbText).toBeVisible();
    }).toPass({ timeout: 20000, intervals: [1000, 2000, 3000] });
    
    // Then check "Webhook Details" tab is visible
    await expect(webhookViewPage.webhookDetailsTab).toBeVisible({ timeout: 10000 });
    
    // Finally check webhook name appears in page title
    await expect(webhookViewPage.pageTitle.filter({ hasText: createdWebhookName })).toBeVisible({ timeout: 10000 });
    
    // Wait for page to be fully loaded
    await page.waitForLoadState('load');

    // STEP 3: VERIFY WEBHOOK DATA MATCHES INPUT DATA
    
    // Perform comprehensive verification
    const verificationResults = await webhookViewPage.verifyCompleteWebhookData(testData);
    
    // Assert basic details verification
    expect(verificationResults.basicDetails.nameMatches, 
      `Webhook name mismatch. Expected: "${testData.validWebhookName}", Actual: "${verificationResults.basicDetails.actualName}"`
    ).toBe(true);
    
    expect(verificationResults.basicDetails.descriptionMatches, 
      `Webhook description mismatch. Expected: "${testData.validDescription}", Actual: "${verificationResults.basicDetails.actualDescription}"`
    ).toBe(true);
    
    // Verify webhook URL exists - using verification results
    expect(verificationResults.basicDetails.urlExists).toBe(true);

    // Assert field mappings verification
    expect(verificationResults.fieldMappings.projectMatches,
      `Project mapping mismatch. Expected: "${testData.projectName}", Actual: "${verificationResults.fieldMappings.actualProject}"`
    ).toBe(true);
    
    expect(verificationResults.fieldMappings.issueTypeMatches,
      `Issue type mapping mismatch. Expected: "${testData.issueType}", Actual: "${verificationResults.fieldMappings.actualIssueType}"`
    ).toBe(true);
    
    expect(verificationResults.fieldMappings.summaryMatches,
      `Summary template mismatch. Expected: "${testData.summaryTemplate}", Actual: "${verificationResults.fieldMappings.actualSummary}"`
    ).toBe(true);
    
    expect(verificationResults.fieldMappings.descriptionMatches,
      `Description template mismatch. Expected: "${testData.descriptionTemplate}", Actual: "${verificationResults.fieldMappings.actualDescription}"`
    ).toBe(true);
    
  });
});