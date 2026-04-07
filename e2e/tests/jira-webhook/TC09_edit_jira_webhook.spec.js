import { test, expect } from '@playwright/test';
import { POManager } from '../../pages/Common_Files/POManager.js';
import { JiraWebhookCreatorPage } from '../../pages/Jira_Webhook/CreateJiraWebhookPage.js';
import { JiraWebhookEditPage } from '../../pages/Jira_Webhook/EditJiraWebhook.js';
import { JiraWebhookTestCaseDesc } from '../../TestCases/JiraWebhookTestCaseDesc.js';
import { GenerateTestData } from '../../utils/GenerateTestData.js';

test.describe('Jira Webhook Edit - End to End Edit Workflow', () => {
  let editPage;
  let jiraWebhookCreator;
  let testDataGenerator;
  let testData;

  test.beforeEach(async ({ page }) => {
    // Initialize test data generator
    testDataGenerator = new GenerateTestData();
    testData = testDataGenerator.getBasicDetailsTestData();
    
    // Initialize page objects
    editPage = new JiraWebhookEditPage(page);
    jiraWebhookCreator = new JiraWebhookCreatorPage(page);
    
    const poManager = new POManager(page);
    await poManager.loginPage.pageUrlAsync();
    await poManager.loginPage.loginAsync();
  });

  test(JiraWebhookTestCaseDesc.editJiraWebhookTestCase, async ({ page }) => {
    // Step 1: Create a new webhook first using the consolidated method
    await jiraWebhookCreator.createJiraWebhook(testData);
    
    // Step 2: Navigate to edit the created webhook
    await page.getByRole('button', { name: 'Webhook options' }).first().click();
    await page.getByRole('menuitem', { name: 'Edit' }).click();
    
    // Step 3: Update Basic Details
    // Note: Webhook name is immutable in edit mode - only description can be updated
    const updatedDescription = 'Updated description after edit';
    await editPage.descriptionInput.fill(updatedDescription);
    
    // Proceed to Step 2 - Sample Payload
    await editPage.proceedToNextStep();
    
    // Update JSON payload
    const newPayload = '{"issue":{"summary":"Updated test issue for editing","description":"Updated payload test","priority":"High","category":"Bug Fix","department":"Engineering","reportedBy":"Test User","environment":"Production"},"metadata":{"timestamp":"2026-01-12T15:30:00Z","source":"automated-test","severity":"medium"}}';
    await editPage.updateJsonPayload(newPayload);
    
    // Proceed to Step 3 - Connect Jira (connection already verified for existing webhook)
    await editPage.proceedToNextStep();
    
    // Step 4: Connection handling - check if already verified or needs verification
    const poManager = new POManager(page);
    
    // Check if connection is already verified or if we need to verify it
    const isAlreadyVerified = await poManager.basePage.ui.buttons.verified.isVisible({ timeout: 3000 }).catch(() => false);
    
    if (!isAlreadyVerified) {
      // If not verified, we may need to verify the connection
      const verifyButton = page.getByRole('button', { name: 'Verify Connection' });
      const isVerifyButtonVisible = await verifyButton.isVisible({ timeout: 3000 }).catch(() => false);
      
      if (isVerifyButtonVisible) {
        await verifyButton.click();
        await expect(poManager.basePage.ui.buttons.verified).toBeVisible({ timeout: 15000 });
      }
    }
    
    // Proceed to Step 4 - Field Mapping
    await editPage.proceedToNextStep();
    
    // Step 5: Update Field Mapping
    await editPage.updateFieldMapping('KIP Test Project B', 'Task', 
      'Updated Summary: {{issue.summary}} - Priority: {{issue.priority}}',
      'Updated Description: {{issue.description}} | Department: {{issue.department}} | Reported By: {{issue.reportedBy}}'
    );
    
    // Proceed to Step 5 - Review & Update
    await editPage.proceedToNextStep();
    
    // Step 6: Verify Review Page Content - ONLY ASSERTIONS KEPT HERE
    await expect(page.getByText('Review your webhook configuration')).toBeVisible();
    
    // Verify updated templates appear in Template Preview section
    await expect(page.getByText('Updated Summary: {{issue.summary}} - Priority: {{issue.priority}}')).toBeVisible();
    await expect(page.getByText('Updated Description: {{issue.description}} | Department: {{issue.department}} | Reported By: {{issue.reportedBy}}')).toBeVisible();
    
    // Step 7: Update webhook
    await poManager.basePage.ui.getButtonByText('Update Webhook').click();
    
    // Verify successful update notification appears
    await expect(page.getByText(/Jira Webhook.*updated successfully/)).toBeVisible({ timeout: 10000 });
    
    // Verify we're back on the webhook list page
    await expect(poManager.basePage.ui.buttons.copyUrl.first()).toBeVisible();
  });
});