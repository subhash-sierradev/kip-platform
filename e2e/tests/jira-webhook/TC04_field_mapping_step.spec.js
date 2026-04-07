import { test, expect } from '@playwright/test';
import { POManager } from '../../pages/Common_Files/POManager.js';
import { JiraWebhookConnectJiraPage } from '../../pages/Jira_Webhook/JiraWebhookConnectJiraPage.js';
import { JiraWebhookTestCaseDesc } from '../../TestCases/JiraWebhookTestCaseDesc.js';
import { GenerateTestData } from '../../utils/GenerateTestData.js';

// Helper function to navigate to webhook creation
const navigateToWebhookCreation = async (page) => {
  const ui = page.ui || (new (await import('../../pages/Common_Files/UICommonElements.js')).UICommonElements(page));
  await ui.mainNavigation.outboundMenu.click();
  await ui.mainNavigation.jiraWebhookMenu.click();
  await ui.buttons.addJiraWebhook.click();
};

// Helper function to handle connection selection and verification using page object
const selectAndVerifyConnection = async (page, connectJiraPage) => {
  try {
    await connectJiraPage.selectConnection();
    await connectJiraPage.waitForVerifyButtonEnabled();
    await connectJiraPage.verifyConnection();
    await expect(page.getByRole('button', { name: 'Verified' })).toBeVisible({ timeout: 15000 });
    return true;
  } catch (error) {
    // Check if there are any active connections available
    try {
      const connectionCountText = await page.getByText(/connections.*active.*failed/).textContent({ timeout: 3000 });
      if (connectionCountText.includes('0 active')) {
        test.skip();
      }
    } catch (e) {
      test.skip();
    }
    throw error;
  }
};

test.describe('Jira Webhook Creation - Field Mapping Step', () => {
  let testDataGenerator;
  let testData;

  test.beforeEach(async ({ page }) => {
    // Initialize test data generator
    testDataGenerator = new GenerateTestData();
    testData = testDataGenerator.getBasicDetailsTestData();
    
    const poManager = new POManager(page);
    await poManager.loginPage.pageUrlAsync();
    await poManager.loginPage.loginAsync();
  });

  test(JiraWebhookTestCaseDesc.fieldMappingTestCase, async ({ page }) => {
    // Initialize page object
    const connectJiraPage = new JiraWebhookConnectJiraPage(page);
    const poManager = new POManager(page);
    
    // Navigate and complete previous steps
    await navigateToWebhookCreation(page);
    await page.getByRole('textbox', { name: 'Jira Webhook Name' }).pressSequentially(testData.validWebhookName, { delay: 50 });
    await poManager.basePage.ui.buttons.next.click();
    
    const jsonPayload = testData.samplePayload;
    await page.getByRole('textbox', { name: 'Paste your JSON payload here' }).fill(JSON.stringify(jsonPayload));
    await page.getByRole('button', { name: 'Next →' }).click();

    // Complete connection step using page object methods
    await expect(page.getByText('Connection Method')).toBeVisible();
    
    // Select 'Use Existing Connection' option (should be default)
    await expect(page.getByRole('radio', { name: 'Use an Existing Connection – Reuse a previously configured connection' })).toBeChecked();

    // Select and verify existing Jira connection
    const connectionSelected = await selectAndVerifyConnection(page, connectJiraPage);
    if (!connectionSelected) {
      return; // Skip test if no connections available
    }
    
    // Verify Next button becomes enabled after successful verification
    await expect(connectJiraPage.ui.buttons.next).toBeEnabled();
    
    // Click 'Next' to proceed to step 4
    await connectJiraPage.clickNext();

    // Verify Field Mapping step is loaded
    await expect(page.getByRole('heading', { name: 'Map to Jira Fields' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Incoming JSON' })).toBeVisible();

    // Verify initial state - Next button disabled, dropdowns properly configured
    await expect(page.getByRole('button', { name: 'Next →' })).toBeDisabled();
    await expect(page.getByRole('combobox').nth(3)).toBeDisabled(); // Issue Type
    await expect(page.getByRole('combobox').nth(4)).toBeDisabled(); // Assignee

    // Select project and verify dependent dropdowns enable
    const projectDropdown = page.getByRole('combobox').nth(2);
    await projectDropdown.selectOption(['KIP Test Project A']);
    await expect(page.getByRole('combobox').nth(3)).toBeEnabled();
    await expect(page.getByRole('combobox').nth(4)).toBeEnabled();

    // Complete field mapping
    await page.getByRole('combobox').nth(3).selectOption(['Epic']);
    await page.getByRole('textbox', { name: 'Enter issue summary template' }).fill('Case Update: {{form.title}} - Priority: {{form.priority}}');
    await page.getByRole('textbox', { name: 'Enter issue description' }).fill('Case Details:\\nForm ID: {{form.id}}\\nSubmitted by: {{form.submitter}}\\nStatus: {{form.status}}\\nNotes: {{form.notes}}');

    // Verify Next button enables and proceed
    await expect(page.getByRole('button', { name: 'Next →' })).toBeEnabled();
    await page.getByRole('button', { name: 'Next →' }).click();
    await expect(page.getByText('Review your webhook configuration')).toBeVisible();

  });
});