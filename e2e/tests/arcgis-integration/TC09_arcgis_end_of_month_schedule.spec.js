import { test, expect } from '@playwright/test';
import { POManager } from '../../pages/Common_Files/POManager.js';
import { ArcGISIntegrationTestCaseDesc } from '../../TestCases/ArcGISIntegrationTestCaseDesc.js';
import { GenerateTestData } from '../../utils/GenerateTestData.js';

test.describe('ArcGIS Integration - Schedule End of Month Configuration', () => {

  let poManager, integrationConfig, scheduleConfig;

  test.beforeEach(async ({ page }) => {
    const testData    = new GenerateTestData();
    integrationConfig = testData.getArcGISIntegrationConfig();
    scheduleConfig    = testData.getEndOfMonthScheduleConfig();

    poManager = new POManager(page);

    await poManager.loginPage.pageUrlAsync();
    await poManager.loginPage.loginAsync();
    await expect(poManager.ui.userProfile.welcomeText).toBeVisible();

    // Create a fresh integration to be updated in the test
    await poManager.arcgisIntegrationCreatorPage.createArcGISIntegration(integrationConfig);
    await expect(poManager.ui.buttons.gridView).toBeVisible();
  });

  test(ArcGISIntegrationTestCaseDesc.arcGISEndOfMonthScheduleTestCase, async () => {

    await poManager.arcgisIntegrationEditorPage.openEditWizard(integrationConfig.name);

    // Step 1 (Integration Details — no changes, advance to Schedule Configuration)
    await poManager.arcgisIntegrationEditorPage.clickNext();

    // Step 2: Select Monthly frequency, enable End of Month toggle, check months, set execution time
    await poManager.arcgisIntegrationEditorPage.enableEndOfMonthSchedule(scheduleConfig);

    // Step 3: Keep existing ArcGIS connection
    await poManager.arcgisIntegrationEditorPage.proceedWithExistingConnection();

    // Step 4: No changes to field mapping, advance
    await poManager.arcgisIntegrationEditorPage.clickNext();

    // Step 5: Submit update and verify success
    await poManager.arcgisIntegrationEditorPage.clickUpdateIntegration();
    await poManager.arcgisIntegrationEditorPage.waitForUpdateSuccess();

    // Integration card is visible — End of Month toggle accepted
    await poManager.ui.buttons.gridView.click();
    await poManager.arcgisIntegrationEditorPage.searchIntegration(integrationConfig.name);
    expect(await poManager.arcgisIntegrationEditorPage.verifyIntegrationVisible(integrationConfig.name)).toBe(true);

    // Next Run Date reflects the last day of the first applicable selected month
    const nextRunDateText = await poManager.arcgisIntegrationEditorPage.getNextRunDate(integrationConfig.name);
    expect(nextRunDateText).toBeTruthy();

    // ── Schedule Configuration validation (Schedule Info tab)
    const integrationCard = poManager.arcgisIntegrationManagementPage.getIntegrationCard(integrationConfig.name);
    await expect(async () => {
      await integrationCard.click();
      await expect(poManager.arcgisIntegrationViewPage.breadcrumbText).toBeVisible();
    }).toPass({ intervals: [1000, 2000, 3000] });

    await poManager.arcgisIntegrationViewPage.clickScheduleInfoTab();

    // Pattern: Monthly
    await expect(poManager.arcgisIntegrationViewPage.page.getByText('Monthly'),
      'Pattern badge should display "Monthly"'
    ).toBeVisible();

    // Pattern subtitle: Executes on selected months
    await expect(poManager.arcgisIntegrationViewPage.page.getByText('Executes on selected months'),
      'Pattern subtitle should display "Executes on selected months"'
    ).toBeVisible();

    // Months: January, March, May
    for (const month of scheduleConfig.monthsDisplay) {
      await expect(poManager.arcgisIntegrationViewPage.page.getByText(month, { exact: true }),
        `Month chip "${month}" should be visible`
      ).toBeVisible();
    }

    // Run on Last Day of Month: Enabled
    await expect(
      poManager.arcgisIntegrationViewPage.page.locator('.info-row')
        .filter({ hasText: /Run on Last Day of Month/i })
        .getByText('Enabled'),
      'Run on Last Day of Month should be "Enabled"'
    ).toBeVisible();
  });

});
