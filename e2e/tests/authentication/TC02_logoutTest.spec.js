import { test, expect } from '@playwright/test';
import { POManager } from '../../pages/Common_Files/POManager.js';
import { AuthenticationTestCaseDesc } from '../../TestCases/AuthenticationTestCaseDesc.js';

test(AuthenticationTestCaseDesc.logoutTestCase, async ({ page }) => {
  const poManager = new POManager(page);
  
  // Navigate to login page and login
  await poManager.loginPage.pageUrlAsync();
  await poManager.loginPage.loginAsync();
  
  // Perform logout
  await poManager.loginPage.logoutAsync();
  
  // Verify user is redirected to login page
  await expect(poManager.basePage.ui.headings.signInHeading).toBeVisible();
  
  // Try to access protected page to verify session is terminated
  await page.goto('https://kaseware.sierradev.com/outbound/webhook/jira');
  
  // Verify login page displays without any user context after session termination
  await expect(poManager.basePage.ui.headings.signInHeading).toBeVisible();
});