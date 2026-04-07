import { BasePage } from '../Common_Files/BasePage.js';

class JiraWebhookEditPage extends BasePage {
  constructor(page) {
    super(page);
    
    // Static locators for Edit Interface
    this.updateWebhookHeading = page.getByRole('heading', { name: 'Update Jira Webhook' });
    this.webhookNameInput = page.getByRole('textbox', { name: 'Jira Webhook Name' });
    this.descriptionInput = page.getByRole('textbox', { name: 'Provide a brief description' });
    this.jsonPayloadInput = page.getByRole('textbox', { name: 'Paste your JSON payload here' });
    this.formatJsonButton = page.getByRole('button', { name: '✨ Format JSON' });
    this.nextButton = page.getByRole('button', { name: 'Next →' });
    this.previousButton = page.getByRole('button', { name: '← Previous' });
    this.updateWebhookButton = page.getByRole('button', { name: 'Update Webhook' });
    this.closeButton = page.getByRole('button', { name: '✕' });
    
    // Connection step locators
    this.useExistingConnectionRadio = page.getByRole('radio', { name: 'Use Existing Connection – Select from saved connections' });
    this.createNewConnectionRadio = page.locator('label').filter({ hasText: 'Create New Connection - Set' });
    this.connectionDropdown = page.locator('.cs-dropdown').filter({ hasText: 'Choose a saved connection' });
    this.verifyConnectionButton = page.getByRole('button', { name: 'Verify Connection' });
    this.jiraUrlInput = page.getByRole('textbox', { name: 'e.g. https://your-domain.' });
    this.usernameInput = page.getByRole('textbox', { name: 'Enter your Jira email address' });
    this.passwordInput = page.getByRole('textbox', { name: 'Enter your Jira password or' });
    this.testConnectionButton = page.getByRole('button', { name: 'Test Connection' });
    
    // Field mapping step locators
    this.projectDropdown = page.getByRole('combobox').nth(2);
    this.issueTypeDropdown = page.getByRole('combobox').nth(3);
    this.summaryTemplateInput = page.getByRole('textbox', { name: 'Enter issue summary template' });
    this.descriptionTemplateInput = page.getByRole('textbox', { name: 'Enter issue description' });
    
    // Step indicators and validation
    this.stepCheckmarks = page.getByText('✔');
    this.cancelWarningDialog = page.getByText('Are you sure you want to cancel?');
    this.yesCancelButton = page.getByRole('button', { name: 'Yes, Cancel' });
    this.noGoBackButton = page.getByRole('button', { name: 'No, Go Back' });
  }

  async openEditInterface(webhookOptionsButton) {
    await webhookOptionsButton.click();
    await this.page.getByRole('menuitem', { name: 'Edit' }).click();
    await this.updateWebhookHeading.waitFor();
  }

  async updateBasicDetails(name, description) {
    await this.webhookNameInput.fill(name);
    await this.descriptionInput.fill(description);
  }

  async proceedToNextStep() {
    await this.nextButton.click();
  }

  async updateJsonPayload(payload) {
    await this.jsonPayloadInput.fill(payload);
    await this.formatJsonButton.click();
  }

  async selectExistingConnection() {
    await this.useExistingConnectionRadio.click();
    // Click the dropdown to open it
    const dropdown = this.page.locator('div').filter({ hasText: /^Choose a saved connection$/ }).nth(1);
    await dropdown.click();
    // Wait a moment for the dropdown to open and then select the first option
    await this.page.waitForTimeout(500);
    // Select the first available connection option
    const connectionOption = this.page.getByText(/jira-organization.*Success/).first();
    await connectionOption.click();
  }

  async verifyConnection() {
    await this.verifyConnectionButton.click();
  }

  async setupNewConnection(url, username, password) {
    await this.createNewConnectionRadio.click();
    await this.jiraUrlInput.fill(url);
    await this.usernameInput.fill(username);
    await this.passwordInput.fill(password);
    await this.testConnectionButton.click();
  }

  async updateFieldMapping(project, issueType, summaryTemplate, descriptionTemplate) {
    await this.projectDropdown.selectOption([project]);
    await this.issueTypeDropdown.selectOption([issueType]);
    await this.summaryTemplateInput.fill(summaryTemplate);
    await this.descriptionTemplateInput.fill(descriptionTemplate);
  }

  async closeWithWarning() {
    await this.closeButton.click();
    await this.cancelWarningDialog.waitFor();
    await this.yesCancelButton.click();
  }

  async getStepCheckmarkCount() {
    return await this.stepCheckmarks.count();
  }
}

export { JiraWebhookEditPage };