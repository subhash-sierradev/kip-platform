import { test, expect } from '@playwright/test';
import { POManager } from '../../pages/Common_Files/POManager.js';
import { ArcGISIntegrationTestCaseDesc } from '../../TestCases/ArcGISIntegrationTestCaseDesc.js';
import { GenerateTestData } from '../../utils/GenerateTestData.js';

test.describe('ArcGIS Integration - End-to-End Creation Flow', () => {
  let poManager, arcgisCreatorPage, integrationConfig;

  test.beforeEach(async ({ page }) => {
    integrationConfig = new GenerateTestData().getArcGISIntegrationConfig();
    poManager = new POManager(page);
    arcgisCreatorPage = poManager.arcgisIntegrationCreatorPage;
    await poManager.loginPage.pageUrlAsync();
    await poManager.loginPage.loginAsync();
  });

  test(ArcGISIntegrationTestCaseDesc.arcGISIntegrationCreationTestCase, async () => {
    // Run full creation wizard and verify the new integration card appears in grid view
    await arcgisCreatorPage.createArcGISIntegration(integrationConfig);
    const integrationExists = await arcgisCreatorPage.searchAndValidateIntegration(integrationConfig.name);
    expect(integrationExists).toBe(true);
  });
});
