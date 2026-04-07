import { test, expect } from '@playwright/test';
import { POManager } from '../../pages/Common_Files/POManager.js';
import { ArcGISIntegrationTestCaseDesc } from '../../TestCases/ArcGISIntegrationTestCaseDesc.js';
import { GenerateTestData } from '../../utils/GenerateTestData.js';

test.describe('ArcGIS Integration View Data Verification', () => {
  let poManager;
  let arcgisCreatorPage;
  let arcgisManagementPage;
  let arcgisViewPage;
  let testDataGenerator;
  let integrationConfig;

  test.beforeEach(async ({ page }) => {
    testDataGenerator = new GenerateTestData();
    integrationConfig = testDataGenerator.getArcGISIntegrationConfig();

    poManager = new POManager(page);
    arcgisCreatorPage    = poManager.arcgisIntegrationCreatorPage;
    arcgisManagementPage = poManager.arcgisIntegrationManagementPage;
    arcgisViewPage       = poManager.arcgisIntegrationViewPage;

    // Login
    await poManager.loginPage.pageUrlAsync();
    await poManager.loginPage.loginAsync();
    await expect(poManager.basePage.ui.userProfile.welcomeText).toBeVisible({ timeout: 15000 });
  });

  test(ArcGISIntegrationTestCaseDesc.arcGISViewDataVerificationTestCase, async ({ page }) => {

    // ── STEP 1: Create the ArcGIS integration ────────────────────────────────
    await arcgisCreatorPage.createArcGISIntegration(integrationConfig);

    // Confirm redirect back to the integration list (Grid View button visible)
    await expect(page.getByRole('button', { name: 'Grid View' })).toBeVisible({ timeout: 10000 });

    // ── STEP 2: Search for the created integration ────────────────────────────
    await arcgisManagementPage.searchIntegration(integrationConfig.name);

    // Integration card should be visible after search
    const integrationCard = arcgisManagementPage.getIntegrationCard(integrationConfig.name);
    const integrationText = arcgisManagementPage.getIntegrationText(integrationConfig.name);

    await expect(integrationText).toBeVisible({ timeout: 10000 });
    await expect(integrationCard).toBeVisible({ timeout: 10000 });

    // ── STEP 3: Navigate to the view page ────────────────────────────────────
    // Click the card and wait for the breadcrumb with retry
    await expect(async () => {
      await integrationCard.click();
      await expect(arcgisViewPage.breadcrumbText).toBeVisible();
    }).toPass({ timeout: 20000, intervals: [1000, 2000, 3000] });

    // Basic Details tab and h1 heading visible
    await expect(arcgisViewPage.basicDetailsTab).toBeVisible({ timeout: 10000 });
    await expect(arcgisViewPage.pageTitle.filter({ hasText: integrationConfig.name })).toBeVisible({ timeout: 10000 });

    // Wait for full page load
    await page.waitForLoadState('domcontentloaded');

    // ── STEP 4: Verify Basic Details tab ─────────────────────────────────────
    await arcgisViewPage.clickBasicDetailsTab();

    const actualName = await arcgisViewPage.getIntegrationName();
    expect(actualName,
      `Integration name mismatch. Expected: "${integrationConfig.name}", Actual: "${actualName}"`
    ).toBe(integrationConfig.name);

    const actualDesc = await arcgisViewPage.getDescription();
    expect(actualDesc,
      `Description mismatch. Expected: "${integrationConfig.description}", Actual: "${actualDesc}"`
    ).toBe(integrationConfig.description);

    const actualSubtype = await arcgisViewPage.getItemSubtype();
    expect(actualSubtype,
      `Item subtype mismatch. Expected: "${integrationConfig.itemSubtype}", Actual: "${actualSubtype}"`
    ).toBe(integrationConfig.itemSubtype);

    // ── STEP 5: Verify Schedule Info tab ─────────────────────────────────────
    await arcgisViewPage.clickScheduleInfoTab();

    const actualPattern = await arcgisViewPage.getPattern();
    expect(actualPattern,
      `Pattern mismatch. Expected: "Weekly", Actual: "${actualPattern}"`
    ).toBe('Weekly');

    const actualDays = await arcgisViewPage.getScheduleDayChips();
    for (const day of integrationConfig.schedule.days) {
      expect(actualDays,
        `Expected day chip "${day}" to be present. Actual chips: ${JSON.stringify(actualDays)}`
      ).toContain(day);
    }

    // getStartDate() returns the already-formatted UI string (e.g. "Wed, Feb 25, 2026")
    // formatStartDate() converts YYYY-MM-DD config date to the same format for comparison
    const actualDateRaw = await arcgisViewPage.getStartDate();
    const expectedDate  = arcgisViewPage.formatStartDate(integrationConfig.schedule.startDate);

    expect(actualDateRaw,
      `Start date mismatch. Expected: "${expectedDate}", Actual: "${actualDateRaw}"`
    ).toBe(expectedDate);

    // ── STEP 6: Verify Field Mapping tab ─────────────────────────────────────
    await arcgisViewPage.clickFieldMappingTab();

    // Row 1 (index 0): mandatory system row — id → external_location_id → PASSTHROUGH
    const row1 = await arcgisViewPage.getFieldMappingRow(0);
    expect(row1.kasField,
      `Row 1 Kaseware Field mismatch. Expected: "id", Actual: "${row1.kasField}"`
    ).toContain('id');
    expect(row1.transform,
      `Row 1 Transformation mismatch. Expected: "PASSTHROUGH", Actual: "${row1.transform}"`
    ).toBe('PASSTHROUGH');
    expect(row1.arcgisField,
      `Row 1 ArcGIS Field mismatch. Expected: "external_location_id", Actual: "${row1.arcgisField}"`
    ).toBe('external_location_id');

    // Row 2 (index 1): latitude → Latitude → PASSTHROUGH (first user-mapped field)
    const row2 = await arcgisViewPage.getFieldMappingRow(1);
    expect(row2.kasField,
      `Row 2 Kaseware Field mismatch. Expected: "latitude", Actual: "${row2.kasField}"`
    ).toContain('latitude');
    expect(row2.transform,
      `Row 2 Transformation mismatch. Expected: "PASSTHROUGH", Actual: "${row2.transform}"`
    ).toBe('PASSTHROUGH');
    expect(row2.arcgisField,
      `Row 2 ArcGIS Field mismatch. Expected: "Latitude", Actual: "${row2.arcgisField}"`
    ).toBe('Latitude');
  });
});