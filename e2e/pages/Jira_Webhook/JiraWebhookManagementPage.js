import { expect } from '@playwright/test';
import { BasePage } from '../Common_Files/BasePage.js';
import { UICommonElements } from '../Common_Files/UICommonElements.js';
import { JiraWebhookConnectJiraPage } from './JiraWebhookConnectJiraPage.js';

export class JiraWebhookManagementPage extends BasePage {
  constructor(page) {
    super(page);
    this.uiCommon = new UICommonElements(page);
    
    // Search functionality
    this.searchBox = page.getByPlaceholder('Search webhooks by name...');
    
    // Webhook card elements
    this.webhookOptionsButton = page.getByRole('button', { name: 'Webhook options' });
    this.webhookOptionsButtonByName = (webhookName) =>
      page.locator('.webhook-card')
        .filter({ hasText: webhookName })
        .getByRole('button', { name: 'Webhook options' });
    
    // Status badges
    this.statusChipEnabled = page.locator('.status-chip.status-active');
    this.statusChipDisabled = page.locator('.status-chip.status-disabled');
    
    // Dialog elements
    this.deleteDialog = page.getByRole('dialog').filter({ hasText: /delete|remove/i });
    this.deleteWarningMessage = page.getByText(/This action cannot be undone|permanently delete/i);
    this.disableDialog = page.getByRole('dialog').filter({ hasText: /disable/i });
    this.disableWarningMessage = page.locator('dialog, [role="dialog"]').getByText(/disable|deactivate|webhooks will not|stop processing/i).first();
    this.enableDialog = page.getByRole('dialog').filter({ hasText: /enable|activate/i });
    this.enableWarningMessage = page.locator('dialog, [role="dialog"]').getByText(/enable|activate|webhooks will|start processing/i).first();
    
    // Notification elements - more specific selectors
    this.successNotification = page.locator('.notification, .toast, .alert, [role="alert"], .success-message').filter({ hasText: /success|deleted|created|updated|disabled|enabled/i }).first();
    this.disableSuccessNotification = page.locator('.notification, .toast, .alert, [role="alert"]').filter({ hasText: /disabled successfully|webhook has been disabled/i }).first();
    this.enableSuccessNotification = page.locator('.notification, .toast, .alert, [role="alert"]').filter({ hasText: /enabled successfully|webhook has been enabled/i }).first();
    
    // Connection verification buttons
    this.verifyConnectionButton = page.getByRole('button', { name: 'Verify Connection' });
    this.verifiedButton = page.getByRole('button', { name: 'Verified' });
    
    // Data Grid Elements for History
    this.dataGrid = page.locator('[role="grid"]');
    // Get data rows by excluding rows that contain columnheader
    this.gridRows = page.locator('[role="grid"] [role="row"]:not(:has([role="columnheader"]))');
  }

  //Search for a webhook by name
  async searchWebhook(webhookName) {
    await this.searchBox.waitFor({ state: 'visible', timeout: 5000 });
    await this.searchBox.clear();
    await this.searchBox.pressSequentially(webhookName);
    await this.page.waitForLoadState('domcontentloaded');
  }

  //Common method to click webhook options button for a specific webhook
  async clickWebhookOptions(webhookName) {
    await this.webhookOptionsButtonByName(webhookName).waitFor({ state: 'visible', timeout: 5000 });
    await this.webhookOptionsButtonByName(webhookName).click();
  }

  //Delete a webhook by name
  async deleteWebhook(webhookName) {
    await this.clickWebhookOptions(webhookName);
    await this.page.getByRole('menuitem', { name: 'Delete' }).click();
  }

  //Confirm webhook deletion in the dialog
  async confirmDeletion() {
    await this.uiCommon.buttons.delete.click();
  }

  //Clone a webhook by name
  async cloneWebhook(webhookName) {
    await this.clickWebhookOptions(webhookName);
    await this.page.getByRole('menuitem', { name: 'Clone' }).click();
  }

  //Verify clone page opens and "Copy of" is prepended to webhook name
  async verifyCloneDialogAndGetClonedName(originalWebhookName) {
    await expect(this.uiCommon.getHeadingByText('Clone Jira Webhook')).toBeVisible();
    
    // Note: Clone dialog shows the full webhook name "Copy of Webhook_xxx", not the truncated display name
    // The truncation only affects the list view display, not the actual data
    const expectedClonedName = `Copy of ${originalWebhookName}`;
    const nameInput = this.page.getByRole('textbox', { name: 'Jira Webhook Name' });
    await expect(nameInput).toHaveValue(expectedClonedName);
    
    return expectedClonedName;
  }

  //Optionally update the cloned webhook name and proceed with clone wizard
  async proceedWithCloneWizard(customClonedName) {
    if (customClonedName) {
      const nameInput = this.page.getByRole('textbox', { name: 'Jira Webhook Name' });
      await nameInput.fill(customClonedName);
    }
    await this.uiCommon.buttons.next.click();
  }

  //Complete all remaining steps in the clone wizard (Sample Payload → Connect Jira → Field Mapping → Review & Create)
  async completeCloneWizard() {
    // Step 2: Sample Payload
    await expect(this.uiCommon.buttons.next).toBeVisible();
    await this.uiCommon.buttons.next.click();
    
    // Step 3: Connect Jira - need to verify connection before proceeding
    await expect(this.uiCommon.buttons.next).toBeVisible();
    
    // Verify the connection (connection is pre-selected from cloned webhook)
    // Always click Verify Connection button for cloned webhooks
    await this.verifyConnectionButton.waitFor({ state: 'visible', timeout: 5000 });
    await this.verifyConnectionButton.click();
    
    // Wait for verification to complete
    await expect(this.verifiedButton).toBeVisible({ timeout: 15000 });
    
    // Wait for Next button to be enabled after verification
    await expect(this.uiCommon.buttons.next).toBeEnabled({ timeout: 5000 });
    await this.uiCommon.buttons.next.click();
    
    // Step 4: Field Mapping
    await expect(this.uiCommon.buttons.next).toBeVisible();
    await this.uiCommon.buttons.next.click();
    
    // Step 5: Review & Create
    await expect(this.uiCommon.buttons.createWebhook).toBeVisible();
    await this.uiCommon.buttons.createWebhook.click();
  }

  //Verify clone success dialog appears and click OK to return to webhook list
  async verifyCloneSuccessAndReturn() {
    await expect(this.uiCommon.buttons.okUnderstand).toBeEnabled();
    await this.uiCommon.buttons.okUnderstand.click();
  }

  //Open options menu for a webhook and click Disable
  async disableWebhook(webhookName) {
    await this.clickWebhookOptions(webhookName);
    await this.page.getByRole('menuitem', { name: 'Disable' }).click();
  }

  //Confirm disable action in the disable confirmation dialog
  async confirmDisable() {
    await this.uiCommon.getButtonByText('Disable').click();
  }

  //Open options menu for a webhook and click Enable
  async enableWebhook(webhookName) {
    await this.clickWebhookOptions(webhookName);
    await this.page.getByRole('menuitem', { name: 'Enable' }).click();
  }

  //Confirm enable action in the enable confirmation dialog
  async confirmEnable() {
    await this.uiCommon.getButtonByText('Enable').click();
  }

  //Returns the status badge locator (Enabled/Disabled) for a specific webhook card
  getWebhookStatusBadge(webhookName, status) {
    // Map status text to CSS class
    const statusClass = status.toLowerCase() === 'enabled' ? '.status-active' : '.status-disabled';
    
    // Find webhook card container by the webhook-card class, then filter by name
    const webhookCard = this.page.locator('.webhook-card').filter({ hasText: webhookName });
    
    // Return the status chip within that card
    return webhookCard.locator(`.status-chip${statusClass}`);
  }

  //Clear the search box and search for a specific webhook by name
  async clearAndSearchWebhook(webhookName) {
    await this.searchBox.clear();
    await this.searchWebhook(webhookName);
  }

  //Returns a locator that matches exact webhook name text on the page
  getWebhookText(webhookName) {
    return this.page.getByText(webhookName, { exact: true });
  }

  //Returns the full webhook card element filtered by webhook name
  getWebhookCard(webhookName) {
    return this.page.locator('.webhook-card').filter({ hasText: webhookName });
  }

  //Returns the clickable webhook name text inside a card, used for navigating to webhook details
  getWebhookCardNameForNavigation(webhookName) {
    // Find within webhook-grid > webhook-card > exact text
    return this.page.locator('.webhook-grid')
      .locator('.webhook-card')
      .filter({ hasText: webhookName })
      .getByText(webhookName, { exact: true });
  }

  //Test the webhook, verify success notification, check history tab and validate filtered records by status
  async validateWebhookTestAndHistory(expectedRecordData, filterStatus = 'SUCCESS') {
    // Click Test Webhook button
    const testButton = this.uiCommon.getButtonByText('Test Webhook');
    await testButton.click();
    
    // Verify success notification
    const notification = this.page.locator('text=/Webhook test completed successfully/i');
    await expect(notification).toBeVisible({ timeout: 15000 });
    
    // Navigate to Webhook History tab
    const historyTab = this.uiCommon.getTab('Webhook History');
    await historyTab.click();
    await this.dataGrid.waitFor({ state: 'visible', timeout: 10000 });
    
    // Verify first history record data if expected values provided
    if (expectedRecordData) {
      const firstRow = this.gridRows.first();
      await firstRow.waitFor({ state: 'visible', timeout: 10000 });
      
      const cells = firstRow.locator('[role="gridcell"]');
      
      if (expectedRecordData.status) {
        const status = await cells.nth(2).textContent();
        expect(status?.trim()).toContain(expectedRecordData.status);
      }
    }
    
    // Apply status filter
    await this.page.getByRole('button', { name: "Show filter options for column 'Status'" }).click();
    
    const filterDialog = this.page.getByRole('dialog', { name: 'Filter options' });
    const filterOption = filterDialog.getByRole('option', { name: filterStatus });
    await filterOption.click();
    
    const okButton = filterDialog.getByRole('button', { name: 'OK' });
    await okButton.click();
    await filterDialog.waitFor({ state: 'hidden', timeout: 5000 });
    
    // Verify filtered records contain expected status
    const allRows = await this.gridRows.all();
    expect(allRows.length).toBeGreaterThan(0);
    
    for (const row of allRows) {
      const statusCell = row.locator('[role="gridcell"]').nth(2);
      await expect(statusCell).toContainText(filterStatus);
    }
  }
}
