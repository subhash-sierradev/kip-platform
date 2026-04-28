import { test, expect } from '@playwright/test';
import { POManager } from '../../pages/Common_Files/POManager.js';
import { ConfluenceIntegrationTestCaseDesc } from '../../TestCases/ConfluenceIntegrationTestCaseDesc.js';
import { GenerateTestData } from '../../utils/GenerateTestData.js';

test.describe('Confluence Integration - Update (Daily Window)', () => {

  let poManager, confluenceCreatorPage, confluenceEditorPage, integrationConfig, updateConfig;

  test.beforeEach(async ({ page }) => {
    const testData    = new GenerateTestData();
    integrationConfig = testData.getConfluenceIntegrationConfig();
    updateConfig      = testData.getConfluenceUpdateConfig();

    poManager             = new POManager(page);
    confluenceCreatorPage = poManager.confluenceIntegrationCreatorPage;
    confluenceEditorPage  = poManager.confluenceIntegrationEditorPage;

    await poManager.loginPage.pageUrlAsync();
    await poManager.loginPage.loginAsync();
    await expect(poManager.ui.userProfile.welcomeText).toBeVisible({ timeout: 15000 });

    // Create a fresh integration to be updated in the test
    await confluenceCreatorPage.createConfluenceIntegration(integrationConfig);
    // Confirm management page loaded — card existence is validated inside openEditWizard
    await expect(poManager.ui.buttons.gridView).toBeVisible({ timeout: 10000 });
  });

  test(ConfluenceIntegrationTestCaseDesc.confluenceIntegrationUpdateTestCase, async () => {

    await confluenceEditorPage.openEditWizard(integrationConfig.name);

    // Step 1: Update name, description, item subtype
    await confluenceEditorPage.updateBasicDetails(
      updateConfig.newName,
      updateConfig.newDescription,
      updateConfig.newItemSubtype
    );

    // Step 2: Change schedule mode to Daily Window
    await confluenceEditorPage.updateScheduleConfiguration(updateConfig.schedule);

    // Step 3: Keep existing connection — test and proceed
    await confluenceEditorPage.proceedWithExistingConnection();

    // Step 4: Keep existing Confluence space configuration
    await confluenceEditorPage.proceedWithExistingConfluenceConfig();

    // Step 5: Submit update and verify success
    await confluenceEditorPage.clickUpdateIntegration();
    await confluenceEditorPage.waitForUpdateSuccess();

    // Verify updated integration name appears in Grid View
    expect(await confluenceCreatorPage.searchAndValidateIntegration(updateConfig.newName)).toBe(true);
  });

});
