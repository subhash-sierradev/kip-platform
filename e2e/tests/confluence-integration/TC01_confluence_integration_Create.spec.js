import { test, expect } from '@playwright/test';
import { POManager } from '../../pages/Common_Files/POManager.js';
import { ConfluenceIntegrationTestCaseDesc } from '../../TestCases/ConfluenceIntegrationTestCaseDesc.js';
import { GenerateTestData } from '../../utils/GenerateTestData.js';

test.describe('Confluence Integration - Creation (Happy Path)', () => {
  let poManager, confluenceCreatorPage, integrationConfig;

  test.beforeEach(async ({ page }) => {
    integrationConfig = new GenerateTestData().getConfluenceIntegrationConfig();
    poManager = new POManager(page);
    confluenceCreatorPage = poManager.confluenceIntegrationCreatorPage;
    await poManager.loginPage.pageUrlAsync();
    await poManager.loginPage.loginAsync();
  });

  test(ConfluenceIntegrationTestCaseDesc.confluenceIntegrationCreationTestCase, async () => {
    await confluenceCreatorPage.createConfluenceIntegration(integrationConfig);
    const integrationExists = await confluenceCreatorPage.searchAndValidateIntegration(integrationConfig.name);
    expect(integrationExists).toBe(true);
  });
});
