import { test, expect } from '@playwright/test';
import { POManager } from '../../pages/Common_Files/POManager.js';
import { JiraWebhookCreatorPage } from '../../pages/Jira_Webhook/CreateJiraWebhookPage.js';
import { JiraWebhookTestCaseDesc } from '../../TestCases/JiraWebhookTestCaseDesc.js';
import { GenerateTestData } from '../../utils/GenerateTestData.js';

test.describe('Jira Webhook Creation - End to End Webhook Creation', () => 
{
  let jiraWebhookCreator;
  let testDataGenerator;
  let testData;

  test.beforeEach(async ({ page }) => {
    // Initialize test data generator
    testDataGenerator = new GenerateTestData();
    testData = testDataGenerator.getBasicDetailsTestData();
    
    // Initialize page objects
    jiraWebhookCreator = new JiraWebhookCreatorPage(page);
    
    const poManager = new POManager(page);
    await poManager.loginPage.pageUrlAsync();
    await poManager.loginPage.loginAsync();
  });

  test(JiraWebhookTestCaseDesc.endToEndWebhookCreationTestCase, async ({ page }) => 
  {
    // Create Jira webhook using consolidated method
    await jiraWebhookCreator.createJiraWebhook(testData);
  });
});