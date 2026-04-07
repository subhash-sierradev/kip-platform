import { test } from '@playwright/test';
import { POManager } from '../../pages/Common_Files/POManager.js';
import { JiraWebhookReviewCreatePage } from '../../pages/Jira_Webhook/JiraWebhookReviewCreatePage.js';
import { JiraWebhookTestCaseDesc } from '../../TestCases/JiraWebhookTestCaseDesc.js';
import { GenerateTestData } from '../../utils/GenerateTestData.js';

test.describe('Jira Webhook Creation - Review and Create Step', () => {
  let page;
  let reviewCreatePage;
  let testData;

  test.beforeEach(async ({ page: testPage }) => {
    page = testPage;
    
    // Login to application
    const poManager = new POManager(page);
    await poManager.loginPage.pageUrlAsync();
    await poManager.loginPage.loginAsync();
    
    // Initialize page object and test data
    reviewCreatePage = new JiraWebhookReviewCreatePage(page);
    testData = new GenerateTestData().getBasicDetailsTestData();
  });

  test(JiraWebhookTestCaseDesc.reviewCreateTestCase, async () => {
    // Navigate to webhook creation page
    await reviewCreatePage.navigateToWebhookCreation();
    
    // Step 1: Fill Basic Details
    await reviewCreatePage.fillBasicDetails(
      testData.validWebhookName, 
      testData.validDescription
    );

    // Step 2: Add Sample Payload
    await reviewCreatePage.fillSamplePayload(testData.samplePayload);

    // Step 3: Connect to Jira
    await reviewCreatePage.selectAndVerifyConnection();

    // Step 4: Configure Field Mapping
    await reviewCreatePage.fillFieldMapping(
      'KIP Test Project A',
      'Epic',
      'Case Update: {{form.title}} - Priority: {{form.priority}}',
      'Case Details:\\nForm ID: {{form.id}}\\nSubmitted by: {{form.submitter}}\\nStatus: {{form.status}}\\nNotes: {{form.notes}}'
    );

    // Step 5: Review Configuration
    await reviewCreatePage.verifyReviewPage(
      testData.validWebhookName, 
      'KIP Test Project A', 
      'Epic'
    );

    // Create the webhook
    await reviewCreatePage.createWebhook();

    // Verify creation success
    await reviewCreatePage.verifyWebhookCreationSuccess();
    
    // Close success dialog
    await reviewCreatePage.acknowledgeCreation();

    // Verify webhook appears in the list
    await reviewCreatePage.verifyWebhookInList();
  });
});