import { expect } from '@playwright/test';
import { BasePage } from '../Common_Files/BasePage.js';

class ArcGISIntegrationCreatorPage extends BasePage {
  constructor(page) {
    super(page);

    this.modal = page.locator('.jw-modal-backdrop');

    // Step 1: Basic Details
    this.nameInput = page.getByPlaceholder('Enter ArcGIS integration name');
    this.descriptionInput = page.getByPlaceholder('Provide a brief description');
    this.itemSubtypeCombobox = this.modal.getByRole('combobox').nth(1); 
    
    // Step 2: Schedule Configuration
    this.dataWindowDropdown = this.modal.getByRole('combobox').first();
    this.frequencyCombobox = this.modal.getByRole('combobox').nth(1);
    this.dayCheckbox = (dayName) => page.getByRole('checkbox', { name: dayName, exact: true });
    this.dateInput = this.modal.getByPlaceholder('mm-dd-yyyy');
    this.timeInput = this.modal.getByRole('textbox').last();
    this.cronExpressionInput = this.modal.locator('input[placeholder*="0 0 9"]');

    // Step 3: Connection
    this.connectionDropdownTrigger = page.locator('.cs-dropdown-selected').first();
    this.connectionOption = (name) => page.locator('.cs-connection-name').filter({ hasText: name }).first();
    this.firstActiveConnectionOption = page.locator('.cs-connection-item').filter({ hasText: /Active/i }).first();
    this.testConnectionButton = page.getByRole('button', { name: 'Test Connection' });
    this.connectionSuccessMessage = page.getByRole('button', { name: 'Verified' });

    // Step 4: Field Mapping
    this.fieldMappingComboboxes = this.modal.getByRole('combobox');

    // Step 5: Review & Create
    this.createIntegrationButton = page.getByRole('button', { name: 'Create ArcGIS Integration' });
    this.creationSuccessMessage = page.getByText('ArcGIS Integration created successfully');

    this.addIntegrationButton = page.getByRole('button', { name: '+ Add ArcGIS Integration' });
    this.searchInput = page.getByPlaceholder('Search integrations by name...');
    this.integrationCard = (name) => page.locator('.integration-card').filter({ hasText: name }).first();

    this.dayMap = {
      'Monday': 'Mon', 'Tuesday': 'Tue', 'Wednesday': 'Wed',
      'Thursday': 'Thu', 'Friday': 'Fri', 'Saturday': 'Sat', 'Sunday': 'Sun'
    };
  }

  async completeBasicDetails(integrationName, description = '', itemSubtype = null) {
    await this.nameInput.fill(integrationName);
    if (description) await this.descriptionInput.fill(description);
    if (itemSubtype) {
      await this.itemSubtypeCombobox.selectOption({ label: itemSubtype });
    }
    await this.clickNext();
  }

  async completeWeeklySchedule(startDate, executionTime, days, dataWindow = null) {
    if (!startDate) {
      const d = new Date();
      d.setDate(d.getDate() + 1);
      startDate = d.toISOString().split('T')[0];
    }
    if (dataWindow) await this.dataWindowDropdown.selectOption({ label: dataWindow });
    await this.frequencyCombobox.selectOption({ label: 'Weekly' });
    for (const day of days) {
      const checkbox = this.dayCheckbox(this.dayMap[day] || day);
      if (await checkbox.isVisible()) await checkbox.check();
    }
    await this.dateInput.fill(startDate);
    await this.timeInput.fill(executionTime);
    await this.clickNext();
  }

  async completeCronSchedule(cronExpression, dataWindow = null) {
    if (dataWindow) await this.dataWindowDropdown.selectOption({ label: dataWindow });
    await this.frequencyCombobox.selectOption({ label: 'CRON' });
    await this.cronExpressionInput.fill(cronExpression);
    await this.clickNext();
  }

  // Routes to completeCronSchedule or completeWeeklySchedule based on schedule.frequency
  async completeScheduleConfiguration(scheduleConfig) {
    const frequency = scheduleConfig?.frequency?.toUpperCase();
    switch (frequency) {
      case 'CRON':
        await this.completeCronSchedule(scheduleConfig.cronExpression, scheduleConfig?.dataWindow ?? null);
        break;
      case 'WEEKLY':
      default:
        await this.completeWeeklySchedule(
          scheduleConfig?.startDate ?? null,
          scheduleConfig.executionTime,
          scheduleConfig.days,
          scheduleConfig?.dataWindow ?? null
        );
        break;
    }
  }

  // Selects connection by name or first Active entry, tests it, then advances
  async completeUseExistingConnection(connectionName = null) {
    await this.click(this.connectionDropdownTrigger);
    const option = connectionName
      ? this.connectionOption(connectionName)
      : this.firstActiveConnectionOption;
    await this.click(option);
    await this.click(this.testConnectionButton);
    await this.connectionSuccessMessage.waitFor({ timeout: 15000 });
    await this.clickNext();
  }

  async completeFieldMapping(mappings) {
    for (const mapping of mappings) {
      await this.fieldMappingComboboxes.nth(3).selectOption({ label: mapping.documentField });
      await this.fieldMappingComboboxes.nth(4).selectOption({ label: mapping.transformation });
      await this.fieldMappingComboboxes.nth(5).selectOption({ label: mapping.arcgisField });
    }
    await this.clickNext();
  }

  async completeReviewAndCreate() {
    await this.click(this.createIntegrationButton);
    await this.creationSuccessMessage.waitFor({ timeout: 15000 });
  }

  async clickNext() {
    await this.ui.buttons.next.click();
  }

  // Orchestrates all wizard steps end-to-end using the provided config object
  async createCompleteIntegration(config) {
    await this.completeBasicDetails(config.name, config.description, config.itemSubtype);
    await this.completeScheduleConfiguration(config.schedule);
    const connectionName = config.connection?.selectFirstActive ? null : config.connection?.name ?? null;
    await this.completeUseExistingConnection(connectionName);
    await this.completeFieldMapping(config.fieldMappings);
    await this.completeReviewAndCreate();
  }

  async createArcGISIntegration(config) {
    await this.navigateToArcGISIntegration();
    await this.openCreationWizard();
    await this.createCompleteIntegration(config);
  }

  async navigateToArcGISIntegration() {
    await this.click(this.ui.mainNavigation.outboundMenu);
    await this.click(this.ui.mainNavigation.arcgisMenu);
  }

  async openCreationWizard() {
    await this.click(this.addIntegrationButton);
  }

  async searchAndValidateIntegration(integrationName) {
    if (await this.ui.buttons.gridView.isVisible()) await this.ui.buttons.gridView.click();
    await this.searchInput.fill(integrationName);
    await expect(this.integrationCard(integrationName)).toBeVisible({ timeout: 10000 });
  }
}

export { ArcGISIntegrationCreatorPage };
