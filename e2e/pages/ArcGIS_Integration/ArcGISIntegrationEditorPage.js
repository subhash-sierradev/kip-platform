import { expect } from '@playwright/test';
import { BasePage } from '../Common_Files/BasePage.js';

export class ArcGISIntegrationEditorPage extends BasePage {
  constructor(page) {
    super(page);

    this.modal = page.locator('.jw-modal-backdrop');

    // Step 1: Basic Details
    this.nameInput = page.getByPlaceholder('Enter ArcGIS integration name');
    this.descriptionInput = page.getByPlaceholder('Provide a brief description');
    this.itemSubtypeCombobox = this.modal.getByRole('combobox').nth(1); // nth(0) is disabled Item Type

    // Step 2: Schedule Configuration — nth(0) is Data Window, nth(1) is Frequency
    this.frequencyCombobox = this.modal.getByRole('combobox').nth(1);
    this.endOfMonthToggle = page.locator('#execute-on-month-end');
    this.endOfMonthToggleButton = page.locator('label[for="execute-on-month-end"]');
    this.monthCheckbox = (monthName) => page.getByRole('checkbox', { name: monthName, exact: true });
    this.dateInput = this.modal.getByPlaceholder('mm-dd-yyyy');
    this.timeInput = this.modal.getByRole('textbox').last();

    this.nextRunDateByName = (integrationName) =>
      page.locator('.integration-card').filter({ hasText: integrationName })
        .locator('.info-cell.align-right .value').first();

    // Step 3: Connection
    this.testConnectionButton = page.getByRole('button', { name: 'Test Connection' });

    // Step 4: Field Mapping
    this.addRowButton = page.locator('button.fm-btn-add').last();
    this.fieldMappingComboboxes = this.modal.getByRole('combobox');

    // Step 5: Review & Update
    this.updateIntegrationButton = page.getByRole('button', { name: /Update ArcGIS Integration/i });
    this.updateSuccessMessage = page.locator('.toast.toast-success')
      .filter({ hasText: /updated successfully|Integration updated/i }).first();

    this.searchInput = page.getByPlaceholder('Search integrations by name...');
    this.integrationCard = (name) => page.locator('.integration-card').filter({ hasText: name }).first();
    this.modalCloseButton = page.locator('.jw-icon-button').filter({ hasText: '✕' }).first();
    this.integrationOptionsButtonByName = (integrationName) =>
      page.locator('.integration-card').filter({ hasText: integrationName })
        .getByRole('button', { name: 'Integration options' });
  }

  async clickNext() {
    await this.ui.buttons.next.click();
  }

  async openEditWizard(integrationName) {
    await this.searchInput.clear();
    await this.searchInput.fill(integrationName);

    await this.click(this.integrationOptionsButtonByName(integrationName));
    await this.page.getByRole('menuitem', { name: 'Edit' }).click();
    await expect(this.nameInput).toBeVisible();
    await this.dismissAllNotifications();
  }

  async updateBasicDetails(newName, newDescription = '', newItemSubtype = null, newDynamicDocument = '') {
    await this.nameInput.clear();
    await this.nameInput.fill(newName);
    if (newDescription) {
      await this.descriptionInput.clear();
      await this.descriptionInput.fill(newDescription);
    }
    if (newItemSubtype) {
      await this.updateDocumentSelection(newItemSubtype, newDynamicDocument);
    } else {
      await this.ui.buttons.next.click();
    }
  }

  // Selects item subtype; for Dynamic Document also picks the specified or first available doc
  async updateDocumentSelection(newItemSubtype, newDynamicDocument = '') {
    await this.itemSubtypeCombobox.click();
    await this.itemSubtypeCombobox.selectOption({ label: newItemSubtype });

    if (newItemSubtype === 'Dynamic Document') {
      const dynCombobox = this.page.locator('select').filter({ hasText: 'Select Dynamic Document' });
      await expect(dynCombobox).toBeVisible();

      await expect(async () => {
        const count = await dynCombobox.locator('option:not([disabled]):not([value=""])').count();
        expect(count).toBeGreaterThan(0);
      }).toPass();

      if (newDynamicDocument) {
        await dynCombobox.evaluate((sel, label) => {
          const opt = Array.from(sel.options).find(o => o.text === label);
          if (opt) {
            sel.value = opt.value;
            sel.dispatchEvent(new Event('change', { bubbles: true }));
          }
        }, newDynamicDocument);
      } else {
        await dynCombobox.evaluate(sel => {
          const firstOpt = Array.from(sel.options).find(o => !o.disabled && o.value);
          if (firstOpt) {
            sel.value = firstOpt.value;
            sel.dispatchEvent(new Event('change', { bubbles: true }));
          }
        });
      }

      // wait for React to flush state and enable Next
      await expect(this.page.locator('button.jw-btn.jw-btn-primary')).toBeEnabled();
    }

    await this.ui.buttons.next.click();
  }

  async enableEndOfMonthSchedule({ frequency, months = [], executionTime }) {
    await this.frequencyCombobox.selectOption({ label: frequency });

    await expect(this.endOfMonthToggleButton).toBeVisible();
    if (!(await this.endOfMonthToggle.isChecked())) {
      await this.endOfMonthToggleButton.click();
    }

    for (const month of months) {
      const cb = this.monthCheckbox(month);
      await expect(cb).toBeVisible();
      if (!(await cb.isChecked())) await cb.check();
    }

    if (executionTime) {
      await this.timeInput.clear();
      await this.timeInput.fill(executionTime);
    }

    await this.ui.buttons.next.click();
  }

  async updateScheduleConfiguration(frequency, startDate, executionTime, months = []) {
    await this.frequencyCombobox.selectOption({ label: frequency });

    if (frequency === 'Monthly' && months.length > 0) {
      for (const month of months) {
        await this.monthCheckbox(month).check();
      }
    }

    if (startDate) {
      await this.dateInput.clear();
      await this.dateInput.fill(startDate);
    }

    if (executionTime) {
      await this.timeInput.clear();
      await this.timeInput.fill(executionTime);
    }

    await this.ui.buttons.next.click();
  }

  // Tests connection and advances to next step
  async proceedWithExistingConnection() {
    await expect(this.testConnectionButton).toBeVisible();
    await this.testConnectionButton.click();
    await expect(this.ui.buttons.verified).toBeVisible();
    await this.ui.buttons.next.click();
  }

  // Adds a new field mapping row and fills document field, transformation, and ArcGIS field
  async addFieldMappingRow(documentField, transformation, arcgisField) {
    await this.click(this.addRowButton);

    const count = await this.fieldMappingComboboxes.count();
    await this.fieldMappingComboboxes.nth(count - 3).selectOption({ label: documentField });
    await this.fieldMappingComboboxes.nth(count - 2).selectOption({ label: transformation });
    await this.fieldMappingComboboxes.nth(count - 1).selectOption({ label: arcgisField });

    await this.ui.buttons.next.click();
  }

  async clickUpdateIntegration() {
    await this.click(this.updateIntegrationButton);
  }

  // Waits for the update success toast and closes the modal if still open
  async waitForUpdateSuccess() {
    await expect(this.updateSuccessMessage).toBeVisible();

    if (await this.nameInput.isVisible().catch(() => false)) {
      if (await this.modalCloseButton.isVisible().catch(() => false)) {
        await this.modalCloseButton.click();
      }
    }

    await expect(this.nameInput).toBeHidden().catch(() => {});
  }

  async searchIntegration(integrationName) {
    await this.searchInput.clear();
    await this.searchInput.fill(integrationName);
  }

  async verifyIntegrationVisible(integrationName) {
    await expect(this.integrationCard(integrationName)).toBeVisible({ timeout: 10000 });
  }

  async getNextRunDate(integrationName) {
    const el = this.nextRunDateByName(integrationName);
    await expect(el).toBeVisible().catch(() => {});
    return (await el.textContent().catch(() => ''))?.trim() ?? '';
  }
}
