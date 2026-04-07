import { BasePage } from '../Common_Files/BasePage.js';
import { expect } from '@playwright/test';

class AdminAuditLogsPage extends BasePage {
  constructor(page) {
    super(page);
  }

  // Navigation method
  async navigateToAuditLogs() {
    await this.click(this.ui.navigation.adminMenu);
    await this.click(this.ui.navigation.auditLogsMenu);
    // More flexible URL pattern to handle different possible structures
    await this.page.waitForURL('**/admin/**', { waitUntil: 'networkidle' });
  }

  // Grid elements
  get auditGrid() {
    return this.page.locator('[role="grid"]');
  }

  get dataGrid() {
    return this.page.locator('[role="grid"]');
  }

  get gridHeaders() {
    return {
      name: this.page.locator('[role="columnheader"]').filter({ hasText: 'Name' }),
      type: this.page.locator('[role="columnheader"]').filter({ hasText: 'Type' }),
      tenant: this.page.locator('[role="columnheader"]').filter({ hasText: 'Tenant' }),
      user: this.page.locator('[role="columnheader"]').filter({ hasText: 'User' }),
      activity: this.page.locator('[role="columnheader"]').filter({ hasText: 'Activity' }),
      result: this.page.locator('[role="columnheader"]').filter({ hasText: 'Result' }),
      timestamp: this.page.locator('[role="columnheader"]').filter({ hasText: 'Timestamp' })
    };
  }

  // Grid data methods
  async getGridRows() {
    await this.auditGrid.waitFor({ state: 'visible' });
    // Get only data rows, excluding header row
    return this.auditGrid.locator('[role="row"]:not(:has([role="columnheader"]))');
  }

  async getRowData(rowIndex = 0) {
    const rows = await this.getGridRows();
    const row = rows.nth(rowIndex);
    await row.waitFor({ state: 'visible' });
    
    const cells = row.locator('[role="gridcell"]');
    await cells.first().waitFor({ state: 'visible' });
    
    return {
      name: await cells.nth(0).textContent(),
      type: await cells.nth(1).textContent(),
      tenant: await cells.nth(2).textContent(),
      user: await cells.nth(3).textContent(),
      activity: await cells.nth(4).textContent(),
      result: await cells.nth(5).textContent(),
      timestamp: await cells.nth(6).textContent()
    };
  }

  // Validation methods
  async verifyBreadcrumb() {
    const breadcrumb = this.page.locator('nav[aria-label="Breadcrumb"]');
    await expect(breadcrumb).toBeVisible();
  }

  async verifyGridStructureAndData() {
    // Wait for and verify audit grid is visible
    await this.auditGrid.waitFor({ state: 'visible' });
    await expect(this.auditGrid).toBeVisible();
    
    // Verify essential column headers
    await expect(this.gridHeaders.name).toBeVisible();
    await expect(this.gridHeaders.timestamp).toBeVisible();

    // Verify grid contains data
    const rows = await this.getGridRows();
    const rowCount = await rows.count();
    
    if (rowCount > 0) {
      const rowData = await this.getRowData(0);
      expect(rowData.name?.trim()).toBeTruthy();
      expect(rowData.timestamp?.trim()).toBeTruthy();
    }
  }
}

export { AdminAuditLogsPage };