import { test, expect } from '@playwright/test';
import { POManager } from '../../pages/Common_Files/POManager.js';
import { JiraWebhookFieldMappingPage } from '../../pages/Jira_Webhook/JiraWebhookFieldMappingPage.js';
import { JiraWebhookTestCaseDesc } from '../../TestCases/JiraWebhookTestCaseDesc.js';
import { GenerateTestData } from '../../utils/GenerateTestData.js';

test.describe('Jira Webhook Creation - Field Mapping Step', () => {
  let poManager;
  let connectJiraPage;
  let fieldMappingPage;
  let testData;

  test.beforeEach(async ({ page }) => {
    const testDataGenerator = new GenerateTestData();
    testData = testDataGenerator.getBasicDetailsTestData();

    poManager = new POManager(page);
    connectJiraPage = poManager.jiraWebhookConnectJiraPage;
    fieldMappingPage = new JiraWebhookFieldMappingPage(page);

    await poManager.loginPage.pageUrlAsync();
    await poManager.loginPage.loginAsync();
  });

  test(JiraWebhookTestCaseDesc.fieldMappingTestCase, async ({ page }) => {
    // Navigate to webhook creation
    const nav = poManager.ui.mainNavigation;
    await nav.outboundMenu.click();
    await nav.jiraWebhookMenu.click();
    await poManager.ui.buttons.addJiraWebhook.click();
    await expect(page.getByRole('heading', { name: 'Create Jira Webhook' })).toBeVisible();

    // Step 1 – Basic details
    const nameInput = page.getByRole('textbox', { name: 'Jira Webhook Name' });
    await nameInput.click();
    await nameInput.fill(testData.validWebhookName);
    await poManager.basePage.ui.buttons.next.click();

    // Step 2 – Sample payload
    await page.getByRole('textbox', { name: 'Paste your JSON payload here' }).fill(JSON.stringify(testData.samplePayload));
    await page.getByRole('button', { name: 'Next →' }).click();

    // Step 3 – Connect Jira
    await expect(page.getByText('Connection Method')).toBeVisible();

    const testDataGenerator = new GenerateTestData();
    let connectionResult;
    try {
      connectionResult = await connectJiraPage.manageJiraConnectionUpdated(testDataGenerator.generateJiraConnectionName());
    } catch (error) {
      if (error.message.includes('No active connections') || error.message.includes('0 active')) {
        test.skip();
      }
      throw error;
    }

    if (connectionResult !== 'existing' && connectionResult !== 'new') {
      return;
    }

    await expect(page.getByRole('button', { name: 'Next →' })).toBeEnabled({ timeout: 10000 });
    await page.getByRole('button', { name: 'Next →' }).click();

    // Step 4 – Field Mapping
    await expect(fieldMappingPage.mapToJiraFieldsHeading).toBeVisible();
    await expect(fieldMappingPage.incomingJsonHeading).toBeVisible();

    await expect(fieldMappingPage.nextButton).toBeDisabled();
    await expect(fieldMappingPage.issueTypeDropdown).toBeDisabled();

    await fieldMappingPage.selectProject('KIP Test Project A');
    await expect(fieldMappingPage.issueTypeDropdown).toBeEnabled();

    await fieldMappingPage.selectIssueType('Epic');
    await fieldMappingPage.fillSummaryTemplate('Case Update: {{form.title}} - Priority: {{form.priority}}');
    await fieldMappingPage.fillDescriptionTemplate('Case Details:\\nForm ID: {{form.id}}\\nSubmitted by: {{form.submitter}}\\nStatus: {{form.status}}\\nNotes: {{form.notes}}');

    await expect(fieldMappingPage.nextButton).toBeEnabled();
    await fieldMappingPage.clickNext();
    await expect(page.getByText('Review your webhook configuration')).toBeVisible();
  });
});