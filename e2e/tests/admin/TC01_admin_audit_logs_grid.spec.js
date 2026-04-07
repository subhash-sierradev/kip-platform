import { test } from '@playwright/test';
import { POManager } from '../../pages/Common_Files/POManager.js';
import { AdminAuditLogsPage } from '../../pages/Admin_Screens/AdminAuditLogsPage.js';
import { AdminScreenValidations } from '../../TestCases/AdminScreenTestCaseDesc.js';

test.describe('Admin Screens - Audit Logs', () => {
  test(AdminScreenValidations.auditLogsGridValidationTestCase, async ({ page }) => {
    // Initialize page objects
    const poManager = new POManager(page);
    const auditLogsPage = new AdminAuditLogsPage(page);

    // Login using POManager
    await poManager.loginPage.pageUrlAsync();
    await poManager.loginPage.loginAsync();

    // Navigate to Admin > Audit Logs using UI common elements
    await auditLogsPage.navigateToAuditLogs();

    // Verify breadcrumb navigation
    await auditLogsPage.verifyBreadcrumb();

    // Verify complete grid structure and data validation in one method
    await auditLogsPage.verifyGridStructureAndData();
  });
});