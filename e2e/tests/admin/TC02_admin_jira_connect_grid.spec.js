import { test } from '@playwright/test';
import { POManager } from '../../pages/Common_Files/POManager.js';
import { AdminJiraConnectPage } from '../../pages/Admin_Screens/AdminJiraConnectPage.js';
import { AdminScreenValidations } from '../../TestCases/AdminScreenTestCaseDesc.js';

test.describe('Admin Screens - Jira Connect', () => {
  test(AdminScreenValidations.jiraConnectComprehensiveTestCase, async ({ page }) => {
    // Initialize page objects
    const poManager = new POManager(page);
    const jiraConnectPage = new AdminJiraConnectPage(page);

    // Login using POManager
    await poManager.loginPage.pageUrlAsync();
    await poManager.loginPage.loginAsync();

    // Navigate to Admin > Jira Connect using UI common elements
    await jiraConnectPage.navigateToJiraConnect();

    // Verify breadcrumb navigation
    await jiraConnectPage.verifyBreadcrumb();

    // Verify complete Jira Connect functionality in one comprehensive method
    await jiraConnectPage.verifyCompleteJiraConnectFunctionality();
  });
});