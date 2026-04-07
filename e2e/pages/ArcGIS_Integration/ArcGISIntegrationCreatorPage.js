import { BasePage } from '../Common_Files/BasePage.js';

class ArcGISIntegrationCreatorPage extends BasePage {
  constructor(page) {
    super(page);

    this.locators = {
      // uses class*="modal" since the wizard panel has no role="dialog"
      modal: () => this.page.locator('div[role="dialog"], .modal, [class*="modal"]').first(),

      // Step 1: Basic Details
      nameInput: () => this.page.getByPlaceholder('Enter ArcGIS integration name'),
      descriptionInput: () => this.page.getByPlaceholder(/Provide a brief description/i),
      itemSubtypeCombobox: () => this.locators.modal().getByRole('combobox').last(),

      // Step 2: Schedule Configuration
      frequencyCombobox: () => this.locators.modal().getByRole('combobox').first(),
      dayCheckbox: (dayName) => this.page.getByRole('checkbox', { name: dayName, exact: true }),
      dateInput: () => this.page.getByPlaceholder('mm-dd-yyyy'),
      timeInput: () => this.page.getByRole('textbox').last(),
      cronExpressionInput: () => this.page.locator('input.sc-input[type="text"][placeholder*="0 0 9"]'),

      // Step 3: Connection
      connectionDropdownTrigger: () => this.page.locator('.cs-dropdown-selected, .cs-dropdown').filter({ hasText: /Choose a saved connection/i }).first(),
      connectionOption: (name) => {
        const flexiblePattern = name.replace(/[\s\-]+/g, '[\\s\\-]+');
        return this.page.locator('.cs-connection-name', { hasText: new RegExp(flexiblePattern, 'i') }).first();
      },
      // Picks the first dropdown entry with an Active status badge
      firstActiveConnectionOption: () =>
        this.page.locator(
          'li, [role="option"], .cs-connection-item, .cs-dropdown-item, div[class*="connection-item"], div[class*="dropdown-item"]'
        )
          .filter({ has: this.page.locator('.cs-connection-name') })
          .filter({ has: this.ui.statusIndicators.active })
          .first(),
      verifyConnectionButton: () => this.page.getByRole('button', { name: 'Verify Connection' }),
      // Matches success via CSS class or the Verified button state
      connectionSuccessMessage: () => this.page.locator(
        '.cs-verification-message.cs-verification-success, [class*="verification-success"], [class*="success"]'
      ).or(this.ui.buttons.verified).first(),

      // Step 4: Field Mapping
      fieldMappingComboboxes: () => this.locators.modal().getByRole('combobox'),

      // Step 5: Review & Create
      createIntegrationButton: () => this.page.getByRole('button', { name: /Create ArcGIS Integration/i }),
      creationSuccessMessage: () => this.page.locator('text=/Integration created successfully|Success/i'),

      addIntegrationButton: () => this.page.getByRole('button', { name: '+ Add ArcGIS Integration' }),
      searchInput: () => this.page.locator('input[type="search"], input[placeholder*="Search"]').first(),
      integrationCard: (name) => this.page.locator(`text="${name}"`).first()
    };

    this.dayMap = {
      'Monday': 'Mon', 'Tuesday': 'Tue', 'Wednesday': 'Wed',
      'Thursday': 'Thu', 'Friday': 'Fri', 'Saturday': 'Sat', 'Sunday': 'Sun'
    };
  }

  async completeBasicDetails(integrationName, description = '', itemSubtype = null) {
    await this.locators.nameInput().fill(integrationName);
    if (description) await this.locators.descriptionInput().fill(description);
    if (itemSubtype) {
      const combobox = this.locators.itemSubtypeCombobox();
      await combobox.selectOption({ label: itemSubtype });
    }
    await this.clickNext();
  }

  async completeWeeklySchedule(startDate, executionTime, days) {
    if (!startDate) {
      const d = new Date();
      d.setDate(d.getDate() + 1);
      startDate = d.toISOString().split('T')[0];
    }
    await this.locators.frequencyCombobox().selectOption({ label: 'Weekly' });
    for (const day of days) {
      const checkbox = this.locators.dayCheckbox(this.dayMap[day] || day);
      if (await checkbox.isVisible()) await checkbox.check();
    }
    await this.locators.dateInput().fill(startDate);
    await this.locators.timeInput().fill(executionTime);
    await this.clickNext();
  }

  async completeCronSchedule(cronExpression) {
    await this.locators.frequencyCombobox().selectOption({ label: 'CRON' });
    await this.locators.cronExpressionInput().fill(cronExpression);
    await this.clickNext();
  }

  // Routes to completeCronSchedule or completeWeeklySchedule based on schedule.frequency
  async completeScheduleConfiguration(scheduleConfig) {
    const frequency = scheduleConfig?.frequency?.toUpperCase();
    switch (frequency) {
      case 'CRON':
        await this.completeCronSchedule(scheduleConfig.cronExpression);
        break;
      case 'WEEKLY':
      default:
        await this.completeWeeklySchedule(
          scheduleConfig?.startDate ?? null,
          scheduleConfig.executionTime,
          scheduleConfig.days
        );
        break;
    }
  }

  // Selects connection by name or first Active entry, verifies it, then advances
  async completeUseExistingConnection(connectionName = null) {
    await this.click(this.locators.connectionDropdownTrigger());
    const option = connectionName
      ? this.locators.connectionOption(connectionName)
      : this.locators.firstActiveConnectionOption();
    await this.click(option);
    await this.click(this.locators.verifyConnectionButton());
    await this.locators.connectionSuccessMessage().waitFor({ timeout: 15000 });
    await this.clickNext();
  }

  async completeFieldMapping(mappings) {
    for (const mapping of mappings) {
      const comboboxes = this.locators.fieldMappingComboboxes();
      await comboboxes.nth(3).selectOption({ label: mapping.documentField });
      await comboboxes.nth(4).selectOption({ label: mapping.transformation });
      await comboboxes.nth(5).selectOption({ label: mapping.arcgisField });
    }
    await this.clickNext();
  }

  async completeReviewAndCreate() {
    await this.click(this.locators.createIntegrationButton());
    await this.locators.creationSuccessMessage().waitFor({ timeout: 15000 });
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
    await this.click(this.locators.addIntegrationButton());
  }

  async searchAndValidateIntegration(integrationName) {
    if (await this.ui.buttons.gridView.isVisible()) await this.ui.buttons.gridView.click();
    await this.locators.searchInput().fill(integrationName);
    const card = this.locators.integrationCard(integrationName);
    await card.waitFor({ state: 'visible', timeout: 5000 }).catch(() => {});
    return await card.isVisible();
  }
}

export { ArcGISIntegrationCreatorPage };
