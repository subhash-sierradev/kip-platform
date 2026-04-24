import { BasePage } from '../Common_Files/BasePage.js';

class ConfluenceIntegrationCreatorPage extends BasePage {
  constructor(page) {
    super(page);

    // Wizard modal
    this.modal = page.locator('div[role="dialog"], .modal, [class*="modal"]').first();

    // Step 1: Basic Details
    this.nameInput = page.getByPlaceholder('Enter Confluence integration name');
    this.descriptionInput = page.getByPlaceholder(/Provide a brief description/i);
    this.itemSubtypeDropdown = this.modal.getByRole('combobox').last();

    // Step 2: Schedule Configuration
    this.dataWindowModeDropdown = page.locator('select, [role="combobox"]').filter({ hasText: /Daily Window|Rolling Window/ }).first();
    this.frequencyCombobox = page.getByText('Frequency Pattern').locator('..').getByRole('combobox');
    this.rollingWindowSizeInput = page.locator('input[placeholder*="window"], input[type="number"]').first();
    this.dateInput = page.getByPlaceholder('mm-dd-yyyy');
    this.timeInput = page.getByRole('textbox').last();

    // Step 3: Connection
    this.connectionDropdownTrigger = page.locator('.cs-dropdown-selected, .cs-dropdown').filter({ hasText: /Choose a saved connection/i }).first();
    this.firstActiveConnectionOption = page.locator(
      'li, [role="option"], .cs-connection-item, .cs-dropdown-item, div[class*="connection-item"], div[class*="dropdown-item"]'
    )
      .filter({ has: page.locator('.cs-connection-name') })
      .filter({ has: page.getByText('Active') })
      .first();
    this.verifyConnectionButton = page.getByRole('button', { name: 'Test Connection' });
    this.connectionSuccessMessage = page.locator(
      '.cs-verification-message.cs-verification-success, [class*="verification-success"], [class*="success"]'
    ).or(page.getByRole('button', { name: 'Verified' })).first();

    // Step 4: Confluence Configuration
    this.confluenceSpaceDropdown = page.getByRole('combobox', { name: /Select a space/i });
    this.languageDropdown = page.getByRole('combobox', { name: /Select languages/i });
    this.spaceFolderDropdown = page.getByRole('combobox', { name: /Select a folder/i });
    this.tableOfContentsSwitch = page.getByRole('switch', { name: /ON/i });

    // Step 5: Review & Create
    this.createIntegrationButton = page.getByRole('button', { name: /Create Confluence Integration/i });
    this.creationSuccessMessage = page.locator('text=/Integration created successfully|Success/i');

    // Management page
    this.addIntegrationButton = page.getByRole('button', { name: '+ Add Confluence Integration' });
    this.searchInput = page.locator('input[type="search"], input[placeholder*="Search"]').first();
    this.integrationCardByName = (name) => page.locator('.integration-card').filter({ hasText: name }).first();

    // Day checkbox helper
    this.dayCheckboxByName = (dayName) => page.getByRole('checkbox', { name: dayName, exact: true });
  }

  async createConfluenceIntegration(config) {

    // Navigate to Outbound >> Confluence Integration
    await this.click(this.ui.mainNavigation.outboundMenu);
    await this.click(this.ui.mainNavigation.confluenceMenu);

    // Open creation wizard
    await this.click(this.addIntegrationButton);

    // Step 1: Basic Details
    await this.fill(this.nameInput, config.name);
    if (config.description) await this.fill(this.descriptionInput, config.description);
    await this.selectOption(this.itemSubtypeDropdown, config.itemSubtype);
    await this.click(this.ui.buttons.next);

    // Step 2: Schedule Configuration
    await this.selectOption(this.dataWindowModeDropdown, { label: config.schedule.mode });
    await this.selectOption(this.frequencyCombobox, { label: config.schedule.frequency });
    for (const day of config.schedule.days) {
      const checkbox = this.dayCheckboxByName(day);
      if (await checkbox.isVisible()) await checkbox.check();
    }
    if (config.schedule.rollingWindowSize && await this.isVisible(this.rollingWindowSizeInput)) {
      await this.fill(this.rollingWindowSizeInput, config.schedule.rollingWindowSize);
    }
    if (config.schedule.startDate) await this.fill(this.dateInput, config.schedule.startDate);
    if (config.schedule.executionTime) await this.fill(this.timeInput, config.schedule.executionTime);
    await this.click(this.ui.buttons.next);

    // Step 3: Connection - Use Existing
    await this.click(this.ui.formElements.useExistingConnection);
    await this.click(this.connectionDropdownTrigger);
    await this.click(this.firstActiveConnectionOption);
    await this.click(this.verifyConnectionButton);
    await this.click(this.ui.buttons.next);

    // Step 4: Confluence Configuration - select first available space
    await this.click(this.confluenceSpaceDropdown);
    const spaceOptions = this.page.locator('[role="listbox"] [role="option"]');
    await this.click(spaceOptions.first());
    await this.click(this.ui.buttons.next);

    // Step 5: Review & Create
    await this.click(this.createIntegrationButton);

    if (await this.isVisible(this.ui.buttons.okUnderstand)) {
      await this.click(this.ui.buttons.okUnderstand);
    }
  }

  async searchAndValidateIntegration(integrationName) {

    if (await this.isVisible(this.ui.buttons.gridView)) await this.ui.buttons.gridView.click();
    await this.fill(this.searchInput, integrationName);
    
    const card = this.integrationCardByName(integrationName);
    return await card.isVisible();
  }
}

export { ConfluenceIntegrationCreatorPage };
