import { test, expect } from '@playwright/test';
import { POManager } from '../../pages/Common_Files/POManager.js';
import { JiraWebhookTestCaseDesc } from '../../TestCases/JiraWebhookTestCaseDesc.js';
import { GenerateTestData } from '../../utils/GenerateTestData.js';

test.describe('Jira Webhook Edit - End to End Edit Workflow', () => {
  let poManager;
  let testData;

  test.beforeEach(async ({ page }) => {
    testData = new GenerateTestData().getBasicDetailsTestData();

    poManager = new POManager(page);
    await poManager.loginPage.pageUrlAsync();
    await poManager.loginPage.loginAsync();
  });

  test(JiraWebhookTestCaseDesc.editJiraWebhookTestCase, async ({ page }) => {
    const editPage = poManager.editJiraWebhookPage;

    // Step 1: Create a new webhook first
    await poManager.createJiraWebhookPage.createJiraWebhook(testData);

    // Step 2: Open edit for the created webhook
    await page.getByRole('button', { name: 'Webhook options' }).first().click();
    await page.getByRole('menuitem', { name: 'Edit' }).click();

    // Step 3: Update Basic Details (name is immutable - only description)
    const updatedDescription = 'Updated description after edit';
    await editPage.descriptionInput.fill(updatedDescription);
    await editPage.proceedToNextStep();

    // Step 4: Update JSON payload
    const newPayload = '{"issue":{"summary":"Updated test issue for editing","description":"Updated payload test","priority":"High","category":"Bug Fix","department":"Engineering","reportedBy":"Test User","environment":"Production"},"metadata":{"timestamp":"2026-01-12T15:30:00Z","source":"automated-test","severity":"medium"}}';
    await editPage.updateJsonPayload(newPayload);
    await editPage.proceedToNextStep();

    // Step 5: Connection handling - verify if already connected, otherwise test connection
    const isAlreadyVerified = await poManager.basePage.ui.buttons.verified.isVisible({ timeout: 3000 }).catch(() => false);
    if (!isAlreadyVerified) {
      const testButton = page.getByRole('button', { name: 'Test Connection' });
      if (await testButton.isVisible({ timeout: 3000 }).catch(() => false)) {
        await testButton.click();
        await expect(poManager.basePage.ui.buttons.verified).toBeVisible({ timeout: 15000 });
      }
    }
    await editPage.proceedToNextStep();

    // Step 6: Update Field Mapping
    await editPage.updateFieldMapping(
      'KIP Test Project B',
      'Task',
      'Updated Summary: {{issue.summary}} - Priority: {{issue.priority}}',
      'Updated Description: {{issue.description}} | Department: {{issue.department}} | Reported By: {{issue.reportedBy}}'
    );
    await editPage.proceedToNextStep();

    // Step 7: Verify Review Page and submit
    await expect(page.getByText('Review your webhook configuration')).toBeVisible();
    await expect(page.getByText('Updated Summary: {{issue.summary}} - Priority: {{issue.priority}}')).toBeVisible();
    await expect(page.getByText('Updated Description: {{issue.description}} | Department: {{issue.department}} | Reported By: {{issue.reportedBy}}')).toBeVisible();

    await poManager.basePage.ui.getButtonByText('Update Webhook').click();
    await expect(page.getByText(/Jira Webhook.*updated successfully/)).toBeVisible({ timeout: 10000 });
    await expect(poManager.basePage.ui.buttons.copyUrl.first()).toBeVisible();
  });
});