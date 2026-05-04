import { expect } from '@playwright/test';
import { BasePage } from '../Common_Files/BasePage.js';

export class ArcGISIntegrationManagementPage extends BasePage {
  constructor(page) {
    super(page);

    this.searchBox = page.getByPlaceholder('Search integrations by name...');
    this.nameInput = page.getByPlaceholder('Enter ArcGIS integration name');

    this.integrationOptionsButtonByName = (integrationName) =>
      page.locator('.integration-card')
        .filter({ hasText: integrationName })
        .getByRole('button', { name: 'Integration options' });

    // Delete dialog locators
    this.deleteDialog = page.getByRole('dialog').filter({ hasText: /delete|remove/i });
    this.deleteWarningMessage = page.getByText(/This action cannot be undone|permanently delete/i);
    this.deleteDialogTitle = page.getByRole('heading', { name: 'Delete ArcGIS Integration' });
    this.deleteWarningText = page.getByText('Deleting this integration will remove it permanently. This action cannot be undone.');
    this.emptyStateMessage = page.locator('.empty-state').first();

    // Disable/Enable dialog locators
    this.disableDialog = page.getByRole('dialog', { name: 'Disable ArcGIS Integration' });
    this.disableWarningMessage = page.getByText('Disabling this integration will prevent runs until re-enabled.');
    this.enableDialog = page.getByRole('dialog', { name: 'Enable ArcGIS Integration' });
    this.enableWarningMessage = page.getByText('Enabling this integration will allow scheduled or manual runs.');

    // Toast notification locators
    this.cloneSuccessNotification = page.getByText('ArcGIS Integration created successfully');
    this.deleteSuccessNotification = page.locator('.toast.toast-success')
      .filter({ hasText: /deleted successfully|integration deleted/i });
    this.disableSuccessNotification = page.getByText('ArcGIS integration has been disabled and will no longer execute');
    this.enableSuccessNotification = page.getByText('ArcGIS integration has been enabled and is now active');

    this.testConnectionButton = page.getByRole('button', { name: 'Test Connection' });
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

    this.jobHistoryGridRows = page.locator('[role="grid"] [role="row"]');
  }

  async searchIntegration(integrationName) {
    await this.searchBox.waitFor({ state: 'visible', timeout: 5000 });
    await this.searchBox.clear();
    await this.searchBox.fill(integrationName);
  }

  async clickIntegrationOptions(integrationName) {
    await this.integrationOptionsButtonByName(integrationName).waitFor({ state: 'visible', timeout: 5000 });
    await this.integrationOptionsButtonByName(integrationName).click();
  }

  async deleteIntegration(integrationName) {
    await this.clickIntegrationOptions(integrationName);
    await this.page.getByRole('menuitem', { name: 'Delete' }).click();
  }

  async confirmDeletion() {
    await this.ui.buttons.delete.click();
  }

  async cloneIntegration(integrationName) {
    await this.clickIntegrationOptions(integrationName);
    await this.page.getByRole('menuitem', { name: 'Clone' }).click();
  }

  async disableIntegration(integrationName) {
    await this.clickIntegrationOptions(integrationName);
    await this.page.getByRole('menuitem', { name: 'Disable' }).click();
  }

  async confirmDisable() {
    await this.ui.getButtonByText('Disable').click();
  }

  async enableIntegration(integrationName) {
    await this.clickIntegrationOptions(integrationName);
    await this.page.getByRole('menuitem', { name: 'Enable' }).click();
  }

  async confirmEnable() {
    await this.ui.getButtonByText('Enable').click();
  }

  getIntegrationStatusBadge(integrationName, status) {
    const normalized = status.toLowerCase();
    if (normalized !== 'enabled' && normalized !== 'disabled') {
      throw new Error(`Invalid status: "${status}". Expected "Enabled" or "Disabled".`);
    }
    const statusClass = normalized === 'enabled' ? '.status-active' : '.status-disabled';
    return this.page.locator('.integration-card').filter({ hasText: integrationName })
      .locator(`.status-chip${statusClass}`);
  }

  async verifyCloneDialogAndGetClonedName(originalIntegrationName) {
    await expect(this.ui.getHeadingByText('ArcGIS Integration Details')).toBeVisible();
    await expect(this.page.getByText('Review & Clone')).toBeVisible();

    const expectedClonedName = `Copy of ${originalIntegrationName}`;
    await expect(this.nameInput).toHaveValue(expectedClonedName);

    return expectedClonedName;
  }

  async proceedWithCloneWizard(customClonedName) {
    if (customClonedName) await this.nameInput.fill(customClonedName);
    await this.ui.buttons.next.click();
  }

  async completeCloneWizard() {
    // Step 2: Schedule
    await expect(this.ui.buttons.next).toBeVisible();
    await this.ui.buttons.next.click();

    // Step 3: Connection — test before advancing
    await expect(this.ui.buttons.next).toBeVisible();
    await this.testConnectionButton.waitFor({ state: 'visible', timeout: 5000 });
    await this.testConnectionButton.click();
    await expect(this.verifiedButton).toBeVisible({ timeout: 15000 });
    await expect(this.ui.buttons.next).toBeEnabled({ timeout: 5000 });
    await this.ui.buttons.next.click();

    // Step 4: Field Mapping
    await expect(this.ui.buttons.next).toBeVisible();
    await this.ui.buttons.next.click();

    // Step 5: Review & Clone
    await expect(this.cloneIntegrationButton).toBeVisible();
    await this.cloneIntegrationButton.click();
  }

  async verifyCloneSuccessAndReturn() {
    await expect(this.cloneSuccessNotification).toBeVisible({ timeout: 10000 });
    await expect(this.ui.buttons.gridView).toBeVisible({ timeout: 10000 });
  }

  getIntegrationText(integrationName) {
    return this.page.getByText(integrationName, { exact: true });
  }

  getIntegrationCard(integrationName) {
    return this.page.locator('.integration-card').filter({ hasText: integrationName });
  }

  async verifyDeleteConfirmationDialog() {
    await expect(this.deleteDialogTitle).toBeVisible({ timeout: 5000 });
    await expect(this.deleteWarningText).toBeVisible();
    await expect(this.ui.buttons.cancel).toBeVisible();
    await expect(this.ui.buttons.delete).toBeVisible();
  }

  async verifyDeleteSuccessNotification() {
    await expect(this.deleteSuccessNotification).toBeVisible({ timeout: 10000 });
  }

  async verifyIntegrationAbsentFromSearch(integrationName) {
    await this.searchIntegration(integrationName);
    await expect(this.getIntegrationText(integrationName)).not.toBeVisible({ timeout: 5000 });
  }

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

    if (expectedRecordData?.status) {
      await expect(async () => {
        await this.basicDetailsTab.click();
        await this.jobHistoryTab.click();
        await this.jobHistoryGridRows.first().waitFor({ state: 'visible', timeout: 10000 });
        const statusText = await this.jobHistoryGridRows.first().locator('[role="gridcell"]').nth(4).textContent();
        expect(statusText?.trim()).toContain(expectedRecordData.status);
      }).toPass({ timeout: 180000, intervals: [10000, 15000, 20000] });
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
