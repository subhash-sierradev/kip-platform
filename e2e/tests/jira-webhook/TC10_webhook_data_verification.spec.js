import { test, expect } from '@playwright/test';
import { POManager } from '../../pages/Common_Files/POManager.js';
import { JiraWebhookTestCaseDesc } from '../../TestCases/JiraWebhookTestCaseDesc.js';
import { GenerateTestData } from '../../utils/GenerateTestData.js';

test.describe('Jira Webhook Data Verification', () => {
  let poManager;
  let testData;

  test.beforeEach(async ({ page }) => {
    testData = new GenerateTestData().getBasicDetailsTestData();

    poManager = new POManager(page);
    await poManager.loginPage.pageUrlAsync();
    await poManager.loginPage.loginAsync();
    await expect(poManager.basePage.ui.userProfile.welcomeText).toBeVisible({ timeout: 15000 });
  });

  test(JiraWebhookTestCaseDesc.viewWebhookDetailsPageVerificationTestCase, async ({ page }) => {
    const webhookManagementPage = poManager.jiraWebhookManagementPage;
    const webhookViewPage = poManager.jiraWebhookViewPage;

    // STEP 1: CREATE WEBHOOK WITH DYNAMIC TEST DATA
    await poManager.createJiraWebhookPage.createJiraWebhook(testData);

    // STEP 2: NAVIGATE TO WEBHOOK VIEW PAGE
    await webhookManagementPage.searchWebhook(testData.validWebhookName);
    await expect(webhookManagementPage.getWebhookText(testData.validWebhookName)).toBeVisible({ timeout: 10000 });

    const webhookCard = webhookManagementPage.getWebhookCard(testData.validWebhookName);
    await expect(webhookCard).toBeVisible({ timeout: 10000 });

    await expect(async () => {
      await webhookCard.click();
      await expect(webhookViewPage.breadcrumbText).toBeVisible();
    }).toPass({ timeout: 20000, intervals: [1000, 2000, 3000] });

    await expect(webhookViewPage.webhookDetailsTab).toBeVisible({ timeout: 10000 });
    await expect(webhookViewPage.pageTitle.filter({ hasText: testData.validWebhookName })).toBeVisible({ timeout: 10000 });
    await page.waitForLoadState('load');

    // STEP 3: VERIFY WEBHOOK DATA MATCHES INPUT DATA
    const verificationResults = await webhookViewPage.verifyCompleteWebhookData(testData);

    expect(verificationResults.basicDetails.nameMatches,
      `Webhook name mismatch. Expected: "${testData.validWebhookName}", Actual: "${verificationResults.basicDetails.actualName}"`
    ).toBe(true);

    expect(verificationResults.basicDetails.descriptionMatches,
      `Webhook description mismatch. Expected: "${testData.validDescription}", Actual: "${verificationResults.basicDetails.actualDescription}"`
    ).toBe(true);

    expect(verificationResults.basicDetails.urlExists).toBe(true);

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