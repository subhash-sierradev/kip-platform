import { BasePage } from '../Common_Files/BasePage.js';

class AdminNavigationPage extends BasePage {

  constructor(page) {
    super(page);
  }

  async expandAdminMenu() {
    try {
      await this.page.waitForLoadState('load');

      // Check if Clear Cache is already visible (menu already expanded) - use centralized locator
      const clearCacheMenuItem = this.ui.adminNavigation.clearCacheMenuItem;
      const isMenuExpanded = await clearCacheMenuItem.isVisible({ timeout: 2000 }).catch(() => false);

      if (!isMenuExpanded) {
        // Use the centralized locator from UICommonElements
        const adminMenu = this.ui.adminNavigation.adminMenuSelector;
        await adminMenu.click();
        await this.page.waitForLoadState('load');
      }
    } catch (error) {
      throw new Error(`Failed to expand Admin menu: ${error.message}`);
    }
  }

  async navigateToClearCache() {
    try {
      await this.expandAdminMenu();
      await this.page.waitForLoadState('load');

      const element = this.ui.adminNavigation.clearCacheMenuItem;
      await element.click();

      await this.page.waitForLoadState('networkidle');
      const [response] = await Promise.all([
        this.page.waitForResponse(resp => resp.url().includes('/caches/stats')),
        this.page.waitForURL('**/admin/clear-cache', { timeout: 10000 })
      ]);
      if (response.status() !== 200) throw new Error(`HTTP ${response.status()}`);
      await this.page.getByText('Clear Cache').last().waitFor({ state: 'visible' });
    } catch (error) {
      throw new Error(`Failed to navigate to Clear Cache page: ${error.message}`);
    }
  }

  async navigateToCacheStatistics() {
    try {
      await this.expandAdminMenu();
      await this.page.waitForLoadState('load');
      const element = this.ui.adminNavigation.cacheStatisticsMenuItem;
      await element.click();
      await this.page.waitForLoadState('networkidle');
      const [response] = await Promise.all([
        this.page.waitForResponse(resp => resp.url().includes('/caches/stats')),
        this.page.waitForURL('**/admin/cache-statistics', { timeout: 10000 })
      ]);
      if (response.status() !== 200) throw new Error(`HTTP ${response.status()}`);
      await this.page.getByText('Cache Statistics').last().waitFor({ state: 'visible' });
    } catch (error) {
      throw new Error(`Failed to navigate to Cache Statistics page: ${error.message}`);
    }
  }

  async navigateToSiteConfig() {
    try {
      await this.expandAdminMenu();
      await this.page.waitForLoadState('load');
      const element = this.ui.adminNavigation.siteConfigMenuItem;
      await element.click();
      await this.page.waitForLoadState('networkidle');
      const [response] = await Promise.all([
        this.page.waitForResponse(resp => resp.url().includes('/site-configs')),
        this.page.waitForURL('**/admin/site-config', { timeout: 10000 })
      ]);
      if (response.status() !== 200) throw new Error(`HTTP ${response.status()}`);
      await this.page.waitForLoadState('load');
      await this.page.getByText('Site Config').last().waitFor({ state: 'visible' });
    } catch (error) {
      throw new Error(`Failed to navigate to Site Config page: ${error.message}`);
    }
  }

  async verifyClearCachePage() {
    try {
      await this.page.waitForLoadState('load');

      // Check for Clear Cache text in breadcrumb using more specific locator
      const breadcrumbLocator = this.page.locator('span.breadcrumb-text').filter({ hasText: 'Clear Cache' });
      await breadcrumbLocator.waitFor({ state: 'visible', timeout: 10000 });
      const clearCacheVisible = await breadcrumbLocator.isVisible();

      return clearCacheVisible;
    } catch (error) {
      return false;
    }
  }

  async verifyCacheStatisticsPage() {
    try {
      await this.page.waitForLoadState('load');

      // Check for Cache Statistics text in breadcrumb using more specific locator
      const breadcrumbLocator = this.page.locator('span.breadcrumb-text').filter({ hasText: 'Cache Statistics' });
      await breadcrumbLocator.waitFor({ state: 'visible', timeout: 10000 });
      const cacheStatsVisible = await breadcrumbLocator.isVisible();

      return cacheStatsVisible;
    } catch (error) {
      return false;
    }
  }

  async verifySiteConfigPage() {
    try {
      await this.page.waitForLoadState('load');

      // Check for Site Config text in breadcrumb using more specific locator
      const breadcrumbLocator = this.page.locator('span.breadcrumb-text').filter({ hasText: 'Site Config' });
      await breadcrumbLocator.waitFor({ state: 'visible', timeout: 10000 });
      const siteConfigVisible = await breadcrumbLocator.isVisible();

      return siteConfigVisible;
    } catch (error) {
      return false;
    }
  }
}

export { AdminNavigationPage };