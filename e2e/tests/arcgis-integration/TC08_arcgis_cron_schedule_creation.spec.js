import { test, expect } from '@playwright/test';
import { POManager } from '../../pages/Common_Files/POManager.js';
import { ArcGISIntegrationTestCaseDesc } from '../../TestCases/ArcGISIntegrationTestCaseDesc.js';
import { GenerateTestData } from '../../utils/GenerateTestData.js';

test.describe('ArcGIS Integration - CRON Schedule Creation Flow', () => {
  let poManager, arcgisCreatorPage, integrationConfig;

  test.beforeEach(async ({ page }) => {
    integrationConfig = new GenerateTestData().getArcGISCronIntegrationConfig();
    poManager = new POManager(page);
    arcgisCreatorPage = poManager.arcgisIntegrationCreatorPage;
    await poManager.loginPage.pageUrlAsync();
    await poManager.loginPage.loginAsync();
  });

  test(ArcGISIntegrationTestCaseDesc.arcGISCronScheduleCreationTestCase, async ({ page }) => {
    const arcgisManagementPage = poManager.arcgisIntegrationManagementPage;
    const arcgisViewPage       = poManager.arcgisIntegrationViewPage;

    // STEP 1: Create integration with CRON schedule
    await arcgisCreatorPage.createArcGISIntegration(integrationConfig);

    // STEP 2: Search and confirm card is visible in the grid
    if (await poManager.ui.buttons.gridView.isVisible()) await poManager.ui.buttons.gridView.click();
    await arcgisManagementPage.searchIntegration(integrationConfig.name);
    const integrationCard = arcgisManagementPage.getIntegrationCard(integrationConfig.name);
    await expect(integrationCard).toBeVisible({ timeout: 10000 });

    // STEP 3: Click the card and navigate to the view page
    await expect(async () => {
      await integrationCard.click();
      await expect(arcgisViewPage.breadcrumbText).toBeVisible();
    }).toPass({ timeout: 20000, intervals: [1000, 2000, 3000] });

    await page.waitForLoadState('domcontentloaded');

    // STEP 4: Verify Schedule Info tab — pattern should be CUSTOM, CRON matches config
    await arcgisViewPage.clickScheduleInfoTab();

    const actualPattern = await arcgisViewPage.getPattern();
    expect(actualPattern,
      `Pattern mismatch. Expected: "Custom", Actual: "${actualPattern}"`
    ).toBe('Custom');

    const actualPatternDetail = await arcgisViewPage.getPatternDetail();
    expect(actualPatternDetail,
      `Pattern detail mismatch. Expected: "Custom schedule pattern", Actual: "${actualPatternDetail}"`
    ).toBe('Custom schedule pattern');

    const actualCron = await arcgisViewPage.getCronExpression();
    expect(actualCron,
      `CRON expression mismatch. Expected: "${integrationConfig.schedule.cronExpression}", Actual: "${actualCron}"`
    ).toBe(integrationConfig.schedule.cronExpression);
  });
});
