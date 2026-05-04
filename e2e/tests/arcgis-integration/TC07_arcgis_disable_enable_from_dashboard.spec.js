import { test, expect } from '@playwright/test';
import { POManager } from '../../pages/Common_Files/POManager.js';
import { ArcGISIntegrationTestCaseDesc } from '../../TestCases/ArcGISIntegrationTestCaseDesc.js';
import { GenerateTestData } from '../../utils/GenerateTestData.js';

test.describe('ArcGIS Integration Management', () => {
  let integrationConfig;
  let poManager;

  test.beforeEach(async ({ page }) => {
    integrationConfig = new GenerateTestData().getArcGISIntegrationConfig();

    poManager = new POManager(page);

    // Log in with valid credentials and navigate to Kaseware Integration Platform
    await poManager.loginPage.pageUrlAsync();
    await poManager.loginPage.loginAsync();

    // Wait for home page to be fully loaded
    await expect(poManager.ui.userProfile.welcomeText).toBeVisible({ timeout: 15000 });
  });

  test(ArcGISIntegrationTestCaseDesc.disableEnableIntegrationFromDashboardTestCase, async () => {

    // Create a new ArcGIS Integration
    await poManager.arcgisIntegrationCreatorPage.createArcGISIntegration(integrationConfig);

    // Verify Grid View is active after creation
    await expect(poManager.ui.buttons.gridView).toBeVisible();

    // Search for the created integration and verify it appears in the grid
    await poManager.arcgisIntegrationManagementPage.searchIntegration(integrationConfig.name);
    await expect(poManager.arcgisIntegrationManagementPage.getIntegrationText(integrationConfig.name)).toBeVisible();

    // Open Integration options kebab menu and click Disable
    await poManager.arcgisIntegrationManagementPage.disableIntegration(integrationConfig.name);

    // Verify the Disable confirmation dialog and warning message are visible
    await expect(poManager.arcgisIntegrationManagementPage.disableDialog).toBeVisible();
    await expect(poManager.arcgisIntegrationManagementPage.disableWarningMessage).toBeVisible();

    // Confirm disable
    await poManager.arcgisIntegrationManagementPage.confirmDisable();

    // Verify disable success notification
    await expect(poManager.arcgisIntegrationManagementPage.disableSuccessNotification).toBeVisible({ timeout: 10000 });

    // Re-search and verify Disabled status badge
    await poManager.arcgisIntegrationManagementPage.searchIntegration(integrationConfig.name);
    await expect(poManager.arcgisIntegrationManagementPage.getIntegrationText(integrationConfig.name)).toBeVisible({ timeout: 5000 });
    await expect(poManager.arcgisIntegrationManagementPage.getIntegrationStatusBadge(integrationConfig.name, 'Disabled')).toBeVisible();

    // Open Integration options kebab menu and click Enable
    await poManager.arcgisIntegrationManagementPage.enableIntegration(integrationConfig.name);

    // Verify the Enable confirmation dialog and warning message are visible
    await expect(poManager.arcgisIntegrationManagementPage.enableDialog).toBeVisible();
    await expect(poManager.arcgisIntegrationManagementPage.enableWarningMessage).toBeVisible();

    // Confirm enable
    await poManager.arcgisIntegrationManagementPage.confirmEnable();

    // Verify enable success notification
    await expect(poManager.arcgisIntegrationManagementPage.enableSuccessNotification).toBeVisible({ timeout: 10000 });

    // Re-search and verify Enabled status badge
    await poManager.arcgisIntegrationManagementPage.searchIntegration(integrationConfig.name);
    await expect(poManager.arcgisIntegrationManagementPage.getIntegrationText(integrationConfig.name)).toBeVisible({ timeout: 5000 });
    await expect(poManager.arcgisIntegrationManagementPage.getIntegrationStatusBadge(integrationConfig.name, 'Enabled')).toBeVisible();
  });
});
