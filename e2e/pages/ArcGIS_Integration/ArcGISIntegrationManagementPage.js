import { expect } from '@playwright/test';
import { BasePage } from '../Common_Files/BasePage.js';
import { UICommonElements } from '../Common_Files/UICommonElements.js';

export class ArcGISIntegrationManagementPage extends BasePage {
  constructor(page) {
    super(page);
    this.uiCommon = new UICommonElements(page);
    
    this.searchBox = page.getByPlaceholder('Search integrations by name...');

    // Options button scoped to a specific integration card
    this.integrationOptionsButtonByName = (integrationName) =>
      page.locator('.integration-card')
        .filter({ hasText: integrationName })
        .getByRole('button', { name: 'Integration options' });

    // Delete dialog locators
    this.deleteDialog = page.getByRole('dialog').filter({ hasText: /delete|remove/i });
    this.deleteWarningMessage = page.getByText(/This action cannot be undone|permanently delete/i);
    this.deleteDialogTitle = page.getByRole('heading', { name: 'Delete ArcGIS Integration' });
    this.deleteWarningText = page.getByText('Deleting this integration will remove it permanently. This action cannot be undone.');
    this.emptyStateMessage = page.locator('.empty-state, [class*="empty"], [class*="no-results"]').first();

    // Disable/Enable dialog locators
    this.disableDialog = page.getByRole('dialog', { name: 'Disable ArcGIS Integration' });
    this.disableWarningMessage = page.getByText('Disabling this integration will prevent runs until re-enabled.');
    this.enableDialog = page.getByRole('dialog', { name: 'Enable ArcGIS Integration' });
    this.enableWarningMessage = page.getByText('Enabling this integration will allow scheduled or manual runs.');

    // Toast notification locators
    this.cloneSuccessNotification = page.getByText('ArcGIS Integration created successfully');
    this.deleteSuccessNotification = page
      .locator('.toast.toast-success')
      .filter({ hasText: /deleted successfully|integration deleted/i });
    this.disableSuccessNotification = page.getByText('ArcGIS integration has been disabled and will no longer execute');
    this.enableSuccessNotification = page.getByText('ArcGIS integration has been enabled and is now active');

    this.verifyConnectionButton = page.getByRole('button', { name: 'Verify Connection' });
    this.verifiedButton = page.getByRole('button', { name: 'Verified' });

    // Wizard submit buttons
    this.createIntegrationButton = page.getByRole('button', { name: 'Create ArcGIS Integration' });
    this.cloneIntegrationButton = page.getByRole('button', { name: 'Clone ArcGIS Integration' });

    // Job History tab and grid locators
    this.jobHistoryTab = page.getByRole('tab', { name: 'Job History' });
    this.basicDetailsTab = page.getByRole('tab', { name: 'Basic Details' });

    this.runNowButton = page.getByRole('button', { name: 'Run Now' });
    this.runNowDialog = page.getByRole('dialog', { name: 'Run ArcGIS Integration' });
    this.runNowConfirmButton = page.getByLabel('Run ArcGIS Integration').getByRole('button', { name: 'Run Now' });

    // excludes the column-header row
    this.jobHistoryGridRows = page.locator('[role="grid"] [role="row"]');
  }

  // Searches integrations by name using the search box
  async searchIntegration(integrationName) {
    await this.searchBox.waitFor({ state: 'visible', timeout: 5000 });
    await this.searchBox.clear();
    await this.searchBox.fill(integrationName);
    await this.page.waitForLoadState('domcontentloaded');
  }

  // Opens the options menu for the specified integration card
  async clickIntegrationOptions(integrationName) {
    await this.integrationOptionsButtonByName(integrationName).waitFor({ state: 'visible', timeout: 5000 });
    await this.integrationOptionsButtonByName(integrationName).click();
  }

  // Opens options menu and clicks Delete for the given integration
  async deleteIntegration(integrationName) {
    await this.clickIntegrationOptions(integrationName);
    await this.page.getByRole('menuitem', { name: 'Delete' }).click();
  }

  // Confirms deletion in the delete confirmation dialog
  async confirmDeletion() {
    await this.uiCommon.buttons.delete.click();
  }

  // Opens options menu and clicks Clone for the given integration
  async cloneIntegration(integrationName) {
    await this.clickIntegrationOptions(integrationName);
    await this.page.getByRole('menuitem', { name: 'Clone' }).click();
  }

  // Opens options menu and clicks Disable for the given integration
  async disableIntegration(integrationName) {
    await this.clickIntegrationOptions(integrationName);
    await this.page.getByRole('menuitem', { name: 'Disable' }).click();
  }

  // Confirms disable action in the disable confirmation dialog
  async confirmDisable() {
    await this.uiCommon.getButtonByText('Disable').click();
  }

  // Opens options menu and clicks Enable for the given integration
  async enableIntegration(integrationName) {
    await this.clickIntegrationOptions(integrationName);
    await this.page.getByRole('menuitem', { name: 'Enable' }).click();
  }

  // Confirms enable action in the enable confirmation dialog
  async confirmEnable() {
    await this.uiCommon.getButtonByText('Enable').click();
  }

  // Returns the Enabled/Disabled status badge locator for a specific integration card
  getIntegrationStatusBadge(integrationName, status) {
    const normalized = status.toLowerCase();
    if (normalized !== 'enabled' && normalized !== 'disabled') {
      throw new Error(`Invalid status: "${status}". Expected "Enabled" or "Disabled".`);
    }
    const statusClass = normalized === 'enabled' ? '.status-active' : '.status-disabled';
    const integrationCard = this.page.locator('.integration-card').filter({ hasText: integrationName });
    return integrationCard.locator(`.status-chip${statusClass}`);
  }

  async verifyCloneDialogAndGetClonedName(originalIntegrationName) {
    await expect(this.uiCommon.getHeadingByText('ArcGIS Integration Details')).toBeVisible();
    // Verify step is "Review & Clone", not "Review & Create"
    await expect(this.page.getByText('Review & Clone')).toBeVisible();
    
    const expectedClonedName = `Copy of ${originalIntegrationName}`;
    const nameInput = this.page.getByPlaceholder('Enter ArcGIS integration name');
    await expect(nameInput).toHaveValue(expectedClonedName);
    
    return expectedClonedName;
  }

  // Optionally renames the clone and advances to the next wizard step
  async proceedWithCloneWizard(customClonedName) {
    if (customClonedName) {
      const nameInput = this.page.getByPlaceholder('Enter ArcGIS integration name');
      await nameInput.fill(customClonedName);
    }
    await this.uiCommon.buttons.next.click();
  }

  // Completes all remaining steps in the clone wizard (Schedule → Connection → Field Mapping → Review & Clone)
  async completeCloneWizard() {
    // Step 2: Schedule
    await expect(this.uiCommon.buttons.next).toBeVisible();
    await this.uiCommon.buttons.next.click();

    // Step 3: Connection — verify before advancing
    await expect(this.uiCommon.buttons.next).toBeVisible();
    await this.verifyConnectionButton.waitFor({ state: 'visible', timeout: 5000 });
    await this.verifyConnectionButton.click();
    await expect(this.verifiedButton).toBeVisible({ timeout: 15000 });
    await expect(this.uiCommon.buttons.next).toBeEnabled({ timeout: 5000 });
    await this.uiCommon.buttons.next.click();

    // Step 4: Field Mapping
    await expect(this.uiCommon.buttons.next).toBeVisible();
    await this.uiCommon.buttons.next.click();

    // Step 5: Review & Clone
    await expect(this.cloneIntegrationButton).toBeVisible();
    await this.cloneIntegrationButton.click();
  }

  // Waits for clone success toast and redirect back to integration list
  async verifyCloneSuccessAndReturn() {
    await expect(this.cloneSuccessNotification).toBeVisible({ timeout: 10000 });
    await expect(this.page.getByRole('button', { name: 'Grid View' })).toBeVisible({ timeout: 10000 });
  }

  async clearAndSearchIntegration(integrationName) {
    await this.searchIntegration(integrationName);
  }

  // Returns a locator matching the exact integration name text
  getIntegrationText(integrationName) {
    return this.page.getByText(integrationName, { exact: true });
  }

  // Returns the integration card element filtered by name
  getIntegrationCard(integrationName) {
    return this.page.locator('.integration-card').filter({ hasText: integrationName });
  }

  // Verifies the delete confirmation dialog shows title, warning, and action buttons
  async verifyDeleteConfirmationDialog() {
    await expect(this.deleteDialogTitle).toBeVisible({ timeout: 5000 });
    await expect(this.deleteWarningText).toBeVisible();
    await expect(this.uiCommon.buttons.cancel).toBeVisible();
    await expect(this.uiCommon.buttons.delete).toBeVisible();
  }

  async verifyDeleteSuccessNotification() {
    await expect(this.deleteSuccessNotification).toBeVisible({ timeout: 10000 });
  }

  // Verifies integration no longer appears in search results after deletion
  async verifyIntegrationAbsentFromSearch(integrationName) {
    await this.searchIntegration(integrationName);
    await expect(this.getIntegrationText(integrationName)).not.toBeVisible({ timeout: 5000 });
  }

  // Triggers Run Now, verifies a new job history entry is added, then filters and validates row status
  async validateRunNowAndJobHistory(integrationName, expectedRecordData = {}, filterStatus = 'SUCCESS') {
    await this.jobHistoryTab.click();
    const initialRowCount = await this.jobHistoryGridRows.count();

    await this.basicDetailsTab.click();
    await this.runNowButton.click();
    await expect(this.runNowDialog).toBeVisible({ timeout: 5000 });
    await this.runNowConfirmButton.click();
    await expect(this.page.getByText(`${integrationName} has been triggered successfully`)).toBeVisible({ timeout: 15000 });

    await this.jobHistoryTab.click();
    await this.jobHistoryGridRows.first().waitFor({ state: 'visible', timeout: 10000 });
    expect(await this.jobHistoryGridRows.count()).toBeGreaterThan(initialRowCount);

    if (expectedRecordData) {
      // Job processing is async — poll by toggling tabs to force grid refresh
      if (expectedRecordData.status) {
        await expect(async () => {
          await this.basicDetailsTab.click();
          await this.jobHistoryTab.click();
          await this.jobHistoryGridRows.first().waitFor({ state: 'visible', timeout: 10000 });
          const statusText = await this.jobHistoryGridRows.first().locator('[role="gridcell"]').nth(4).textContent();
          expect(statusText?.trim()).toContain(expectedRecordData.status);
        }).toPass({ timeout: 180000, intervals: [10000, 15000, 20000] });
      }
    }

    await this.page.getByRole('button', { name: "Show filter options for column 'Job Status'" }).click();
    const filterDialog = this.page.getByRole('dialog', { name: 'Filter options' });
    await filterDialog.getByRole('option', { name: filterStatus }).click();
    await filterDialog.getByRole('button', { name: 'OK' }).click();
    await filterDialog.waitFor({ state: 'hidden', timeout: 5000 });

    const allRows = await this.jobHistoryGridRows.all();
    expect(allRows.length).toBeGreaterThan(0);
    for (const row of allRows) {
      await expect(row.locator('[role="gridcell"]').nth(4)).toContainText(filterStatus);
    }
  }
}
