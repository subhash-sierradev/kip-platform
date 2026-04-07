import { test, expect } from '@playwright/test';
import { POManager } from '../../pages/Common_Files/POManager.js';
import { ArcGISIntegrationTestCaseDesc } from '../../TestCases/ArcGISIntegrationTestCaseDesc.js';
import { GenerateTestData } from '../../utils/GenerateTestData.js';

test.describe('ArcGIS Integration - Delete from Grid View (Kebab Menu)', () => {

  let integrationConfig;
  let createdIntegrationName;
  let poManager;

  test.beforeEach(async ({ page }) => {
    integrationConfig = new GenerateTestData().getArcGISIntegrationConfig();
    createdIntegrationName = integrationConfig.name;

    poManager = new POManager(page);

    // Step 1: Log in with valid credentials and navigate to Kaseware Integration Platform
    await poManager.loginPage.pageUrlAsync();
    await poManager.loginPage.loginAsync();

    // Wait for home page to be fully loaded
    await expect(poManager.basePage.ui.userProfile.welcomeText).toBeVisible({ timeout: 15000 });
  });

  // ============================================================
  // Delete ArcGIS integration via kebab menu and verify it does not appear in search results after deletion.
  // ============================================================
  test(ArcGISIntegrationTestCaseDesc.deleteIntegrationAndVerifyAbsentTestCase, async () => {

    //Create a new ArcGIS integration
    await poManager.arcgisIntegrationCreatorPage.createArcGISIntegration(integrationConfig);

    // Verify Grid View is active after creation
    await expect(poManager.basePage.ui.buttons.gridView).toBeVisible();

    // Search for the created integration and verify it appears in the grid ──
    await poManager.arcgisIntegrationManagementPage.searchIntegration(createdIntegrationName);
    await expect(poManager.arcgisIntegrationManagementPage.getIntegrationText(createdIntegrationName)).toBeVisible();

    // Open the 'Integration options' kebab menu and click Delete
    await poManager.arcgisIntegrationManagementPage.deleteIntegration(createdIntegrationName);

    // Verify the confirmation dialog — title, warning message, Cancel & Delete buttons
    await poManager.arcgisIntegrationManagementPage.verifyDeleteConfirmationDialog();

    // Confirm deletion by clicking the 'Delete' button in the dialog
    await poManager.arcgisIntegrationManagementPage.confirmDeletion();

    // Verify success notification immediately after confirmation (toast auto-dismisses quickly)
    await poManager.arcgisIntegrationManagementPage.verifyDeleteSuccessNotification();

    // Search for the deleted integration and verify it does not appear in search results
    await poManager.arcgisIntegrationManagementPage.verifyIntegrationAbsentFromSearch(createdIntegrationName);
  });
});
