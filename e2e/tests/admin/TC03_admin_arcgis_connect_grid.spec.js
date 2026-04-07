import { test } from '@playwright/test';
import { POManager } from '../../pages/Common_Files/POManager.js';
import { AdminArcGISConnectPage } from '../../pages/Admin_Screens/AdminArcGISConnectPage.js';
import { AdminScreenValidations } from '../../TestCases/AdminScreenTestCaseDesc.js';

test.describe('Admin Screens - ArcGIS Connect', () => {
  test(AdminScreenValidations.arcgisConnectComprehensiveTestCase, async ({ page }) => {
    // Initialize page objects
    const poManager = new POManager(page);
    const arcgisConnectPage = new AdminArcGISConnectPage(page);

    // Login using POManager
    await poManager.loginPage.pageUrlAsync();
    await poManager.loginPage.loginAsync();

    // Navigate to Admin > ArcGIS Connect using UI common elements
    await arcgisConnectPage.navigateToArcGISConnect();

    // Verify breadcrumb navigation
    await arcgisConnectPage.verifyBreadcrumb();

    // Verify complete ArcGIS Connect functionality in one comprehensive method
    await arcgisConnectPage.verifyCompleteArcGISConnectFunctionality();
  });
});