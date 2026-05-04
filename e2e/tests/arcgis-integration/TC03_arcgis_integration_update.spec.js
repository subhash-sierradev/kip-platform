import { test, expect } from '@playwright/test';
import { POManager } from '../../pages/Common_Files/POManager.js';
import { ArcGISIntegrationTestCaseDesc } from '../../TestCases/ArcGISIntegrationTestCaseDesc.js';
import { GenerateTestData } from '../../utils/GenerateTestData.js';

test.describe('ArcGIS Integration - Edit/Update', () => {

  let poManager, arcgisCreatorPage, arcgisEditorPage, integrationConfig, updateConfig;

  test.beforeEach(async ({ page }) => {
    const testData    = new GenerateTestData();
    integrationConfig = testData.getArcGISIntegrationConfig();
    updateConfig      = testData.getArcGISUpdateConfig();

    poManager        = new POManager(page);
    arcgisCreatorPage = poManager.arcgisIntegrationCreatorPage;
    arcgisEditorPage  = poManager.arcgisIntegrationEditorPage;

    await poManager.loginPage.pageUrlAsync();
    await poManager.loginPage.loginAsync();
    await expect(poManager.ui.userProfile.welcomeText).toBeVisible({ timeout: 15000 });

    // Create a fresh integration to be updated in the test
    await arcgisCreatorPage.createArcGISIntegration(integrationConfig);
    await expect(poManager.ui.buttons.gridView).toBeVisible({ timeout: 10000 });
  });

  test(ArcGISIntegrationTestCaseDesc.updateArcGISIntegration, async () => {

    await arcgisEditorPage.openEditWizard(integrationConfig.name);

    // Step 1: Update name, description and item subtype (all on the same Integration Details step)
    await arcgisEditorPage.updateBasicDetails(updateConfig.newName, updateConfig.newDescription, updateConfig.newItemSubtype);

    // Step 2: Update schedule frequency, months, start date, and execution time
    await arcgisEditorPage.updateScheduleConfiguration(
      updateConfig.schedule.frequency,
      updateConfig.schedule.startDate,
      updateConfig.schedule.executionTime,
      updateConfig.schedule.months
    );

    // Step 3: Keep existing ArcGIS connection
    await arcgisEditorPage.proceedWithExistingConnection();

    // Step 4: Add new field mapping row
    await arcgisEditorPage.addFieldMappingRow(
      updateConfig.newFieldMapping.documentField,
      updateConfig.newFieldMapping.transformation,
      updateConfig.newFieldMapping.arcgisField
    );

    // Step 5: Submit and verify update success
    await arcgisEditorPage.clickUpdateIntegration();
    await arcgisEditorPage.waitForUpdateSuccess();

    // Verify updated name appears in Grid View
    await poManager.ui.buttons.gridView.click();
    await arcgisEditorPage.searchIntegration(updateConfig.newName);
    await arcgisEditorPage.verifyIntegrationVisible(updateConfig.newName);
  });

});

