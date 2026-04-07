import { test, expect } from '@playwright/test';
import { POManager } from '../../pages/Common_Files/POManager.js';
import { AppAdminTestCaseDesc } from '../../TestCases/AppAdminTestCaseDesc.js';

test.describe('App Admin - Navigation Tests', () => {

  test(AppAdminTestCaseDesc.NavigationAdminScreenByAppAdmin, async ({ page }) => {
    const poManager = new POManager(page);
    await poManager.loginPage.pageUrlAsync();
    expect(await poManager.loginPage.isLoaded()).toBe(true);
    await poManager.loginPage.loginAsAppAdminAsync();
    await page.waitForLoadState('networkidle');
    await expect(poManager.basePage.ui.userProfile.welcomeText).toBeVisible({ timeout: 10000 });
    await poManager.adminNavigationPage.navigateToClearCache();
    expect(await poManager.adminNavigationPage.verifyClearCachePage()).toBe(true);
    await poManager.adminNavigationPage.navigateToCacheStatistics();
    expect(await poManager.adminNavigationPage.verifyCacheStatisticsPage()).toBe(true);
    await poManager.adminNavigationPage.navigateToSiteConfig();
    expect(await poManager.adminNavigationPage.verifySiteConfigPage()).toBe(true);
  });

  test(AppAdminTestCaseDesc.NavigationVerifyAppAdminAccessToAdminMenu, async ({ page }) => {
    const poManager = new POManager(page);
    await poManager.loginPage.pageUrlAsync();
    await poManager.loginPage.loginAsAppAdminAsync();
    await page.waitForLoadState('networkidle');
    await expect(poManager.basePage.ui.userProfile.welcomeText).toBeVisible({ timeout: 10000 });
    await poManager.adminNavigationPage.expandAdminMenu();
    expect(await poManager.ui.adminNavigation.clearCacheMenuItem.isVisible()).toBe(true);
    expect(await poManager.ui.adminNavigation.cacheStatisticsMenuItem.isVisible()).toBe(true);
    expect(await poManager.ui.adminNavigation.siteConfigMenuItem.isVisible()).toBe(true);
  });

});