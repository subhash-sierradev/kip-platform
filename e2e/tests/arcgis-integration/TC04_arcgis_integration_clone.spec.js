// spec: Based on Kaseware Integration Platform Test Plan - ArcGIS Integration Actions and Management
// seed: tests/seed.spec.ts

import { test, expect } from '@playwright/test';
import { POManager } from '../../pages/Common_Files/POManager.js';
import { ArcGISIntegrationTestCaseDesc } from '../../TestCases/ArcGISIntegrationTestCaseDesc.js';
import { GenerateTestData } from '../../utils/GenerateTestData.js';

test.describe('ArcGIS Integration Management', () => {

  let integrationConfig;
  let createdIntegrationName;
  let poManager;

  test.beforeEach(async ({ page }) => {
    integrationConfig = new GenerateTestData().getArcGISIntegrationConfig();
    createdIntegrationName = integrationConfig.name;

    poManager = new POManager(page);

    await poManager.loginPage.pageUrlAsync();
    await poManager.loginPage.loginAsync();
    // Wait for page to be ready - increased timeout for repeated test runs
    await expect(poManager.basePage.ui.userProfile.welcomeText).toBeVisible({ timeout: 15000 });
  });

  test(ArcGISIntegrationTestCaseDesc.cloneIntegrationFromDashboardTestCase, async () => {

    // STEP 1: CREATE INTEGRATION FIRST
    await poManager.arcgisIntegrationCreatorPage.createArcGISIntegration(integrationConfig);

    // STEP 2: NOW CLONE THE CREATED INTEGRATION
    // Verify we're back on the integration list page in Grid View
    await expect(poManager.basePage.ui.buttons.gridView).toBeVisible();

    // Search for the created integration by name and verify it's visible
    await poManager.arcgisIntegrationManagementPage.searchIntegration(createdIntegrationName);
    await expect(poManager.arcgisIntegrationManagementPage.getIntegrationText(createdIntegrationName)).toBeVisible();

    // Clone the integration
    await poManager.arcgisIntegrationManagementPage.cloneIntegration(createdIntegrationName);

    // Verify clone dialog opens and "Copy of" is automatically prepended to integration name
    const clonedIntegrationName = await poManager.arcgisIntegrationManagementPage.verifyCloneDialogAndGetClonedName(createdIntegrationName);

    // Verify the expected cloned name format is "Copy of [originalName]"
    expect(clonedIntegrationName).toBe(`Copy of ${createdIntegrationName}`);

    // Proceed with clone wizard (keeps the "Copy of" prefixed name)
    await poManager.arcgisIntegrationManagementPage.proceedWithCloneWizard();

    // Complete the full clone wizard flow (all data is pre-filled from original integration)
    await poManager.arcgisIntegrationManagementPage.completeCloneWizard();

    // Verify clone success and return to integration list
    await poManager.arcgisIntegrationManagementPage.verifyCloneSuccessAndReturn();

    // STEP 3: VERIFY BOTH INTEGRATIONS EXIST AFTER SUCCESSFUL CLONE
    // Clear search to show all integrations
    await poManager.arcgisIntegrationManagementPage.clearAndSearchIntegration('');

    // Verify both original and cloned integrations exist in the list
    await expect(poManager.arcgisIntegrationManagementPage.getIntegrationText(createdIntegrationName)).toBeVisible();
    await expect(poManager.arcgisIntegrationManagementPage.getIntegrationText(clonedIntegrationName)).toBeVisible();

    // Search for cloned integration to verify it was created successfully
    await poManager.arcgisIntegrationManagementPage.clearAndSearchIntegration(clonedIntegrationName);
    await expect(poManager.arcgisIntegrationManagementPage.getIntegrationText(clonedIntegrationName)).toBeVisible();
  });
});

