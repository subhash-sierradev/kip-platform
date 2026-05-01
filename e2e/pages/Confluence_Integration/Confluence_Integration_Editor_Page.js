import { expect } from '@playwright/test';
import { BasePage } from '../Common_Files/BasePage.js';

export class ConfluenceIntegrationEditorPage extends BasePage {
  constructor(page) {
    super(page);

    this.locators = {
      modal: () => this.page.locator('div[role="dialog"], .modal, [class*="modal"]').first(),

      // Step 1: Integration Details
      nameInput: () => this.page.getByPlaceholder('Enter Confluence integration name'),
      descriptionInput: () => this.page.getByPlaceholder(/Provide a brief description/i),
      itemSubtypeCombobox: () =>
        this.locators.modal().locator('.uis-field').filter({ hasText: /Item Subtype/i }).locator('select.uis-input'),

      // Step 2: Schedule Configuration
      dataWindowModeDropdown: () =>
        this.locators.modal().locator('select.sc-input').filter({ hasText: /Daily Window|Rolling Window/ }).first(),
      frequencyCombobox: () =>
        this.locators.modal().locator('.sc-field').filter({ hasText: 'Frequency Pattern' }).locator('select.sc-input'),
      dailyFrequencyCombobox: () =>
        this.locators.modal().locator('.sc-field').filter({ hasText: 'Daily Frequency' }).locator('select.sc-input'),
      rollingWindowSizeInput: () =>
        this.locators.modal().locator('.sc-field').filter({ hasText: /Window Size/i }).locator('input[type="number"]'),
      dateInput: () => this.page.getByPlaceholder('mm-dd-yyyy'),
      timeInput: () => this.page.locator('input[type="time"]'),
      dayCheckboxByName: (dayName) => this.page.getByRole('checkbox', { name: dayName, exact: true }),

      // Step 3: Connection
      testConnectionButton: () => this.page.getByRole('button', { name: 'Test Connection' }),
      verifiedButton: () => this.page.getByRole('button', { name: 'Verified' }),

      // Step 5: Review & Update
      updateIntegrationButton: () =>
        this.page.getByRole('button', { name: /Update Confluence Integration/i }),
      updateSuccessMessage: () =>
        this.page
          .locator('.toast.toast-success')
          .filter({ hasText: /updated successfully|Integration updated/i })
          .first(),

      modalCloseButton: () =>
        this.page.locator('.jw-icon-button').filter({ hasText: '✕' }).first(),
      integrationOptionsButtonByName: (integrationName) =>
        this.page
          .locator('.integration-card')
          .filter({ hasText: integrationName })
          .getByRole('button', { name: 'Integration options' })
    };
  }

  async openEditWizard(integrationName) {
    const searchBox = this.page.getByPlaceholder('Search integrations by name...');
    await searchBox.clear();
    await searchBox.fill(integrationName);

    // Wait for the matching card to appear before interacting — prevents race condition
    await this.page
      .locator('.integration-card')
      .filter({ hasText: integrationName })
      .first()
      .waitFor({ state: 'visible', timeout: 10000 });

    await this.click(this.locators.integrationOptionsButtonByName(integrationName));
    await this.page.getByRole('menuitem', { name: 'Edit' }).click();
    await expect(this.locators.nameInput()).toBeVisible();
    await this.dismissAllNotifications();
  }

  async updateBasicDetails(newName, newDescription = '', newItemSubtype = null) {
    await this.locators.nameInput().clear();
    await this.locators.nameInput().fill(newName);

    if (newDescription) {
      await this.locators.descriptionInput().clear();
      await this.locators.descriptionInput().fill(newDescription);
    }

    if (newItemSubtype) {
      const combo = this.locators.itemSubtypeCombobox();
      await combo.click();
      await combo.selectOption({ label: newItemSubtype });
    }

    await this.ui.buttons.next.click();
  }

  // Supports both Daily Window and Rolling Window modes
  async updateScheduleConfiguration(schedule) {
    await this.locators.dataWindowModeDropdown().selectOption({ label: schedule.mode });
    await this.locators.frequencyCombobox().selectOption({ label: schedule.frequency });

    // Daily Window: skip Daily Frequency since 'Every 24 hours' is selected by default
    // Rolling Window: select only if needed for that mode
    if (schedule.dailyFrequency && schedule.mode === 'Rolling Window') {
      const dailyFreqCombo = this.locators.dailyFrequencyCombobox();
      if (await this.isVisible(dailyFreqCombo)) {
        await dailyFreqCombo.selectOption({ label: schedule.dailyFrequency });
      }
    }

    if (schedule.days && schedule.days.length > 0) {
      for (const day of schedule.days) {
        const cb = this.locators.dayCheckboxByName(day);
        if (await cb.isVisible()) await cb.check();
      }
    }

    if (schedule.rollingWindowSize) {
      const rwInput = this.locators.rollingWindowSizeInput();
      if (await this.isVisible(rwInput)) {
        await rwInput.clear();
        await rwInput.fill(schedule.rollingWindowSize);
      }
    }

    if (schedule.startDate) {
      await this.locators.dateInput().clear();
      await this.locators.dateInput().fill(schedule.startDate);
    }

    if (schedule.executionTime) {
      await this.locators.timeInput().clear();
      await this.locators.timeInput().fill(schedule.executionTime);
    }

    await this.ui.buttons.next.click();
  }

  // Connection is pre-selected in edit mode; test it and proceed
  async proceedWithExistingConnection() {
    const testBtn = this.locators.testConnectionButton();
    await expect(testBtn).toBeVisible();
    await testBtn.click();
    await expect(this.locators.verifiedButton()).toBeVisible();
    await this.ui.buttons.next.click();
  }

  // Keep existing Confluence space configuration and advance
  async proceedWithExistingConfluenceConfig() {
    // Wait for Step 4 to fully load before clicking Next — prevents clicking Step 3's Next again
    await this.page.getByRole('combobox', { name: /Select a space/i }).waitFor({ state: 'visible', timeout: 10000 });
    await this.ui.buttons.next.click();
  }

  async clickUpdateIntegration() {
    await this.click(this.locators.updateIntegrationButton());
  }

  async waitForUpdateSuccess() {
    // Use toPass to retry — toast fades quickly, this handles slow CI timing
    await expect(async () => {
      await expect(this.locators.updateSuccessMessage()).toBeVisible();
    }).toPass({ timeout: 15000 });

    const nameVisible = await this.locators.nameInput().isVisible().catch(() => false);
    if (nameVisible) {
      const closeBtn = this.locators.modalCloseButton();
      if (await closeBtn.isVisible().catch(() => false)) {
        await closeBtn.click();
      }
    }

    // Ensure modal is fully closed before proceeding
    await expect(this.locators.nameInput()).toBeHidden({ timeout: 10000 });
  }

}
