import { test, expect } from '@playwright/test';
import { POManager } from '../../pages/Common_Files/POManager.js';
import { JiraWebhookTestCaseDesc } from '../../TestCases/JiraWebhookTestCaseDesc.js';
import { GenerateTestData } from '../../utils/GenerateTestData.js';

test.describe('Jira Webhook Creation - Navigation and Basic Details', () => {
  let poManager;
  let testData;

  test.beforeEach(async ({ page }) => {
    testData = new GenerateTestData().getBasicDetailsTestData();

    poManager = new POManager(page);
    await poManager.loginPage.pageUrlAsync();
    await poManager.loginPage.loginAsync();
  });

  test(JiraWebhookTestCaseDesc.basicDetailsTestCase, async ({ page }) => {
    const basicDetailsPage = poManager.jiraWebhookBasicDetailsPage;

    await expect(basicDetailsPage.ui.userProfile.welcomeText).toBeVisible();

    await basicDetailsPage.navigateToJiraWebhook();
    await expect(basicDetailsPage.ui.buttons.gridView).toBeVisible();

    await basicDetailsPage.ui.buttons.addJiraWebhook.click();
    await expect(basicDetailsPage.ui.headings.createJiraWebhook).toBeVisible();

    await expect(basicDetailsPage.ui.buttons.next).toBeDisabled();

    await basicDetailsPage.fillWebhookName(testData.validWebhookName);
    await expect(basicDetailsPage.ui.buttons.next).toBeEnabled();

    await basicDetailsPage.fillDescription(testData.validDescription);

    await basicDetailsPage.clickNext();
    await expect(page.getByRole('heading', { name: 'Sample Webhook Payload for Jira Field Mapping' })).toBeVisible();
  });
});