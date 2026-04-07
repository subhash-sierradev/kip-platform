import { test, expect } from '@playwright/test';
import { POManager } from '../../pages/Common_Files/POManager.js';
import { ArcGISIntegrationTestCaseDesc } from '../../TestCases/ArcGISIntegrationTestCaseDesc.js';
import { GenerateTestData } from '../../utils/GenerateTestData.js';

test.describe('ArcGIS Integration - Job History', () => {
  let poManager;
  let testDataGenerator;
  let integrationConfig;

  test.beforeEach(async ({ page }) => {
    testDataGenerator = new GenerateTestData();
    integrationConfig = testDataGenerator.getArcGISIntegrationConfig();

    poManager = new POManager(page);

    await poManager.loginPage.pageUrlAsync();
    await poManager.loginPage.loginAsync();
    await expect(poManager.basePage.ui.userProfile.welcomeText).toBeVisible();

    // Create a fresh ArcGIS integration to use for job history testing
    await poManager.arcgisIntegrationCreatorPage.createArcGISIntegration(integrationConfig);
    await expect(poManager.basePage.ui.buttons.gridView).toBeVisible();
  });

  test(ArcGISIntegrationTestCaseDesc.arcGISJobHistoryTestCase, async ({ page }) => {
    const managementPage = poManager.arcgisIntegrationManagementPage;
    const viewPage = poManager.arcgisIntegrationViewPage;

    await managementPage.searchIntegration(integrationConfig.name);
    const integrationCard = managementPage.getIntegrationCard(integrationConfig.name);

    await expect(async () => {
      await integrationCard.click();
      await expect(viewPage.breadcrumbText).toBeVisible();
    }).toPass({ intervals: [1000, 2000, 3000] });

    await expect(viewPage.basicDetailsTab).toBeVisible();

    await managementPage.validateRunNowAndJobHistory(
      integrationConfig.name,
      { status: 'SUCCESS' },
      'SUCCESS'
    );

    await viewPage.navigateBackToList();
  });
});
