import { test } from '@playwright/test';
import { POManager } from '../../pages/Common_Files/POManager.js';
import { AuthenticationTestCaseDesc } from '../../TestCases/AuthenticationTestCaseDesc.js';

test(AuthenticationTestCaseDesc.loginTestCase, async ({ page }) => {
  const poManager = new POManager(page);
  await poManager.loginPage.pageUrlAsync();
  await poManager.loginPage.loginAsync();
});