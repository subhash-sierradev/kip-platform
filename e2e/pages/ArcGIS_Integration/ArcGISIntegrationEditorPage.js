import { expect } from '@playwright/test';
import { BasePage } from '../Common_Files/BasePage.js';

export class ArcGISIntegrationEditorPage extends BasePage {
  constructor(page) {
    super(page);

    this.locators = {
      modal: () => this.page.locator('div[role="dialog"], .modal, [class*="modal"]').first(),

      // Step 1: Basic Details
      nameInput: () => this.page.getByPlaceholder('Enter ArcGIS integration name'),
      descriptionInput: () => this.page.getByPlaceholder(/Provide a brief description/i),
      itemSubtypeCombobox: () => this.locators.modal().getByRole('combobox').last(),

      // Step 2: Schedule Configuration
      frequencyCombobox: () => this.locators.modal().getByRole('combobox').first(),
      // toggle input is display:none; click label, check state via input
      endOfMonthToggle: () => this.page.locator('#execute-on-month-end'),
      endOfMonthToggleButton: () => this.page.locator('label[for="execute-on-month-end"]'),
      monthCheckbox: (monthName) => this.page.getByRole('checkbox', { name: monthName, exact: true }),
      dateInput: () => this.page.getByPlaceholder('mm-dd-yyyy'),
      timeInput: () => this.page.getByRole('textbox').last(),

      nextRunDateByName: (integrationName) =>
        this.page
          .locator('.integration-card')
          .filter({ hasText: integrationName })
          .locator('.info-cell.align-right .value')
          .first(),

      // Step 3: Connection
      verifyConnectionButton: () => this.page.getByRole('button', { name: 'Verify Connection' }),

      // Step 4: Field Mapping
      addRowButton: () => this.page.locator('button.fm-btn-add').last(),
      fieldMappingComboboxes: () => this.locators.modal().getByRole('combobox'),

      // Step 5: Review & Update
      updateIntegrationButton: () => this.page.getByRole('button', { name: /Update ArcGIS Integration/i }),
      updateSuccessMessage: () =>
        this.page
          .locator('.toast.toast-success')
          .filter({ hasText: /updated successfully|Integration updated/i })
          .first(),

      searchInput: () => this.page.locator('input[type="search"], input[placeholder*="Search"]').first(),
      integrationCard: (name) => this.page.locator(`text="${name}"`).first(),

      modalCloseButton: () => this.page.locator('.jw-icon-button').filter({ hasText: '✕' }).first(),
      integrationOptionsButtonByName: (integrationName) =>
        this.page
          .locator('.integration-card')
          .filter({ hasText: integrationName })
          .getByRole('button', { name: 'Integration options' })
    };
  }

  async clickNext() {
    await this.ui.buttons.next.click();
  }

  async openEditWizard(integrationName) {
    const searchBox = this.page.getByPlaceholder('Search integrations by name...');
    await searchBox.clear();
    await searchBox.fill(integrationName);

    await this.click(this.locators.integrationOptionsButtonByName(integrationName));
    await this.page.getByRole('menuitem', { name: 'Edit' }).click();
    await expect(this.locators.nameInput()).toBeVisible();
    await this.dismissAllNotifications();
  }

  async updateBasicDetails(newName, newDescription = '', newItemSubtype = null, newDynamicDocument = '') {
    await this.locators.nameInput().clear();
    await this.locators.nameInput().fill(newName);
    if (newDescription) {
      await this.locators.descriptionInput().clear();
      await this.locators.descriptionInput().fill(newDescription);
    }
    if (newItemSubtype) {
      // Delegates to updateDocumentSelection so the Dynamic Document combobox
      // (and its required React state flush) are handled before clicking Next.
      await this.updateDocumentSelection(newItemSubtype, newDynamicDocument);
    } else {
      await this.ui.buttons.next.click();
    }
  }

  // Selects item subtype; for Dynamic Document also picks the specified or first available doc
  async updateDocumentSelection(newItemSubtype, newDynamicDocument = '') {
    const subtypeCombobox = this.locators.itemSubtypeCombobox();
    await subtypeCombobox.click();
    await subtypeCombobox.selectOption({ label: newItemSubtype });

    if (newItemSubtype === 'Dynamic Document') {
      const dynCombobox = this.page.locator('select').filter({ hasText: 'Select Dynamic Document' });
      await expect(dynCombobox).toBeVisible();

      await expect(async () => {
        const count = await dynCombobox.locator('option:not([disabled]):not([value=""])').count();
        expect(count).toBeGreaterThan(0);
      }).toPass();

      if (newDynamicDocument) {
        // fires change event so React registers the selection
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
    await this.locators.frequencyCombobox().selectOption({ label: frequency });

    const toggleBtn = this.locators.endOfMonthToggleButton();
    await expect(toggleBtn).toBeVisible();
    const toggleInput = this.locators.endOfMonthToggle();
    if (!(await toggleInput.isChecked())) {
      await toggleBtn.click();
    }

    for (const month of months) {
      const cb = this.locators.monthCheckbox(month);
      await expect(cb).toBeVisible();
      if (!(await cb.isChecked())) {
        await cb.check();
      }
    }

    if (executionTime) {
      await this.locators.timeInput().clear();
      await this.locators.timeInput().fill(executionTime);
    }

    await this.ui.buttons.next.click();
  }

  async updateScheduleConfiguration(frequency, startDate, executionTime, months = []) {
    await this.locators.frequencyCombobox().selectOption({ label: frequency });

    if (frequency === 'Monthly' && months.length > 0) {
      for (const month of months) {
        await this.locators.monthCheckbox(month).check();
      }
    }

    if (startDate) {
      await this.locators.dateInput().clear();
      await this.locators.dateInput().fill(startDate);
    }

    if (executionTime) {
      await this.locators.timeInput().clear();
      await this.locators.timeInput().fill(executionTime);
    }

    await this.ui.buttons.next.click();
  }

  // Verifies connection if the Verify button is present, then advances
  async proceedWithExistingConnection() {
    const verifyBtn = this.locators.verifyConnectionButton();

    // Wait for the connection step to fully render before acting (avoids race condition)
    await expect(verifyBtn).toBeVisible();
    await verifyBtn.click();

    // Wait for the Verified state — relies on default timeout from playwright.config
    await expect(this.ui.buttons.verified).toBeVisible();

    await this.ui.buttons.next.click();
  }

  // Adds a new field mapping row and fills document field, transformation, and ArcGIS field
  async addFieldMappingRow(documentField, transformation, arcgisField) {
    await this.click(this.locators.addRowButton());

    const allComboboxes = this.locators.fieldMappingComboboxes();
    const count = await allComboboxes.count();
    await allComboboxes.nth(count - 3).selectOption({ label: documentField });
    await allComboboxes.nth(count - 2).selectOption({ label: transformation });
    await allComboboxes.nth(count - 1).selectOption({ label: arcgisField });

    await this.ui.buttons.next.click();
  }

  async clickUpdateIntegration() {
    await this.click(this.locators.updateIntegrationButton());
  }

  // Waits for the update success toast and closes the modal if still open
  async waitForUpdateSuccess() {
    await expect(this.locators.updateSuccessMessage()).toBeVisible();

    const nameVisible = await this.locators.nameInput().isVisible().catch(() => false);
    if (nameVisible) {
      const closeBtn = this.locators.modalCloseButton();
      if (await closeBtn.isVisible().catch(() => false)) {
        await closeBtn.click();
      }
    }

    await expect(this.locators.nameInput()).toBeHidden().catch(() => {});
  }

  async searchIntegration(integrationName) {
    await this.locators.searchInput().clear();
    await this.locators.searchInput().fill(integrationName);
  }

  async verifyIntegrationVisible(integrationName) {
    const card = this.locators.integrationCard(integrationName);
    await expect(card).toBeVisible().catch(() => {});
    return await card.isVisible();
  }

  async getNextRunDate(integrationName) {
    const el = this.locators.nextRunDateByName(integrationName);
    await expect(el).toBeVisible().catch(() => {});
    return (await el.textContent().catch(() => ''))?.trim() ?? '';
  }
}
