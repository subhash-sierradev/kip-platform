// pages/JiraWebhookConnectJiraPage.js
import { BasePage } from '../Common_Files/BasePage.js';
import { getJiraCredentials } from '../../utils/JiraCredentials.js';
import { expect } from '@playwright/test';

class JiraWebhookConnectJiraPage extends BasePage {
  constructor(page) {
    super(page);
    
    this.connectionDropdown = page.locator('.cs-dropdown').filter({ hasText: 'Choose a saved connection' });
    this.verifyConnectionButton = page.getByRole('button', { name: 'Test Connection' });
    this.testConnectionButton = page.getByRole('button', { name: 'Test Connection' });
    this.jiraUrlInput = page.getByRole('textbox', { name: 'https://your-domain.atlassian.net' });
    this.connectionNameInput = page.getByRole('textbox', { name: 'Enter connection name' });
    this.emailAddressInput = page.getByRole('textbox', { name: 'your-email@example.com' });
    this.apiTokenInput = page.getByRole('textbox', { name: 'Enter API token (recommended) or password' });
    this.connectJiraHeading = page.getByText('Connect Your Jira Account');
    this.connectionCountText = page.locator('.cs-connection-meta-line');
    
    // Scoped locators for connection management
    this.savedConnectionsSection = page.locator('.cs-section', { hasText: 'Saved Connections' });
    this.scopedDropdown = this.savedConnectionsSection.locator('.cs-dropdown');
    this.scopedDropdownList = this.savedConnectionsSection.locator('.cs-dropdown-list');
  }

  async selectUseExistingConnection() {
    await this.click(this.ui.formElements.useExistingConnection);
  }

  async selectCreateNewConnection() {
    await this.click(this.ui.formElements.createNewConnection);
  }

  async selectConnection(connectionName = null) {
    try {
      // Click to open the dropdown
      await this.connectionDropdown.click();
      
      // Wait for dropdown options to be visible
      await this.page.locator('.cs-dropdown-list, .cs-option').first().waitFor({ state: 'visible', timeout: 5000 });
      
      if (connectionName) {
        // If specific connection name provided, look for it
        const specificConnection = this.page.locator('.cs-connection-name').filter({ hasText: connectionName });
        await specificConnection.click();
      } else {
        // Select the first available connection option
        const firstOption = this.page.locator('.cs-option').first();
        await firstOption.click();
      }
    } catch (error) {
      throw error;
    }
  }

  async verifyConnection() {
    await this.click(this.verifyConnectionButton);
  }

  async fillJiraUrl(url) {
    await this.fill(this.jiraUrlInput, url);
  }

  async fillConnectionName(name) {
    await this.fill(this.connectionNameInput, name);
  }

  async fillEmailAddress(email) {
    await this.fill(this.emailAddressInput, email);
  }

  async fillApiToken(token) {
    await this.fill(this.apiTokenInput, token);
  }

  async fillNewConnectionForm({ jiraUrl, connectionName, email, apiToken }) {
    await this.fillJiraUrl(jiraUrl);
    await this.fillConnectionName(connectionName);
    await this.fillEmailAddress(email);
    await this.fillApiToken(apiToken);
  }

  async testConnection() {
    await this.click(this.testConnectionButton);
  }

  async clickNext() {
    await this.click(this.ui.buttons.next);
  }

  async isConnectionVerified() {
    return await this.isVisible(this.page.getByText('Connection verified'));
  }

  async hasAvailableConnections() {
    try {
      // Wait for the connection section to be visible first
      await this.connectJiraHeading.waitFor({ state: 'visible', timeout: 5000 });
      
      // Click dropdown to open it
      await this.connectionDropdown.click();
      
      // Wait for dropdown options to appear
      await this.page.locator('.cs-dropdown-list, .cs-option').first().waitFor({ state: 'visible', timeout: 3000 });
      
      // Check for connection elements in the opened dropdown
      const connectionElements = this.page.locator('.cs-connection-name');
      const count = await connectionElements.count();
      
      // Close dropdown by pressing Escape
      await this.page.keyboard.press('Escape');
      
      return count > 0;
    } catch (error) {
      return false;
    }
  }

  async waitForVerifyButtonEnabled() {
    // Wait for the specific Test Connection button to be enabled (for existing connections)
    await this.verifyConnectionButton.waitFor({ state: 'visible', timeout: 10000 });
    await expect(this.verifyConnectionButton).toBeEnabled({ timeout: 10000 });
  }

  async waitForTestButtonEnabled() {
    await this.testConnectionButton.waitFor({ state: 'visible', timeout: 10000 });
    await this.page.waitForFunction(() => {
      const buttons = document.querySelectorAll('button');
      const testBtn = Array.from(buttons).find(btn => btn.textContent.includes('Test Connection'));
      return testBtn && !testBtn.disabled;
    }, { timeout: 10000 });
  }

  async hasActiveConnections() {
    return this.hasActiveConnectionsUpdated();
  }

  async manageJiraConnection(jiraConnectionName) {
    const hasActive = await this.hasActiveConnections();

    if (hasActive) {
      try {
        await this.selectConnection();
        await this.waitForVerifyButtonEnabled();
        await this.verifyConnection();
        await this.ui.buttons.verified.waitFor({ state: 'visible', timeout: 15000 });
        const nextReady = await this.isEnabled(this.ui.buttons.next);
        if (nextReady) return 'existing';
      } catch (error) {
        // Fall through to create new connection
      }
    }

    const credentials = getJiraCredentials(jiraConnectionName);
    await this.selectCreateNewConnection();
    await this.fillNewConnectionForm(credentials);
    await this.waitForTestButtonEnabled();
    await this.testConnection();

    try {
      await this.ui.buttons.verified.waitFor({ state: 'visible', timeout: 15000 });
      const nextReady = await this.isEnabled(this.ui.buttons.next);
      if (nextReady) return 'new';
    } catch (error) {
      // fall through to throw
    }

    throw new Error(
      `Jira connection test failed for "${jiraConnectionName}". Verify your .env credentials.`
    );
  }

  async selectConnectionUpdated(connectionName = null) {
    await this.connectionDropdown.click();
    const dropdownList = this.page.locator('.cs-dropdown-list');
    await dropdownList.waitFor({ state: 'visible', timeout: 5000 });

    if (connectionName) {
      await dropdownList.locator('.cs-connection-name', { hasText: connectionName }).click();
    } else {
      await dropdownList.locator('.cs-connection-name').first().click();
    }
  }

  async hasAvailableConnectionsUpdated() {
    try {
      await this.connectJiraHeading.waitFor({ state: 'visible', timeout: 5000 });
      await this.click(this.ui.formElements.useExistingConnection);
      await this.connectionDropdown.waitFor({ state: 'visible', timeout: 5000 });
      await this.connectionDropdown.click();
      await this.page.locator('.cs-dropdown-list, .cs-option').first().waitFor({ state: 'visible', timeout: 3000 });
      
      const connectionElements = this.page.locator('.cs-connection-name');
      const count = await connectionElements.count();
      await this.page.keyboard.press('Escape');
      
      return count > 0;
    } catch (error) {
      return false;
    }
  }

  async hasActiveConnectionsUpdated() {
    try {
      const hasAny = await this.hasAvailableConnectionsUpdated();
      if (!hasAny) return false;

      const countText = await this.connectionCountText.textContent({ timeout: 3000 }).catch(() => null);
      if (!countText) return true;

      return !countText.includes('0 active');
    } catch (error) {
      return false;
    }
  }

  async manageJiraConnectionUpdated(jiraConnectionName) {
    await this.click(this.ui.formElements.useExistingConnection);
    await this.scopedDropdown.waitFor({ state: 'visible', timeout: 5000 });
    await this.scopedDropdown.click();

    let connectionCount = 0;
    try {
      await this.scopedDropdownList.waitFor({ state: 'visible', timeout: 5000 });
      const connectionOptions = this.page.locator('.cs-dropdown-list .cs-option, .cs-dropdown-list [role="option"]');
      connectionCount = await connectionOptions.count();
    } catch {
      // Dropdown list did not appear
    }

    if (connectionCount === 0) {
      await this.page.keyboard.press('Escape');
    }

    // Try the first available connection
    for (let i = 0; i < Math.min(connectionCount, 1); i++) {
      try {
        const connectionOptions = this.page.locator('.cs-dropdown-list .cs-option, .cs-dropdown-list [role="option"]');
        const firstConnection = connectionOptions.first();
        
        await firstConnection.waitFor({ state: 'visible', timeout: 5000 });
        await firstConnection.click();
        await this.waitForTestButtonEnabled();
        await this.testConnection();
        await this.page.getByText(/jira connection successful/i).waitFor({ state: 'visible', timeout: 8000 });

        await this.page.waitForFunction(() => {
          const buttons = document.querySelectorAll('button');
          const nextBtn = Array.from(buttons).find(btn => btn.textContent.includes('Next'));
          return nextBtn && !nextBtn.disabled;
        }, { timeout: 5000 });

        return 'existing';
      } catch (err) {
        // Connection failed, continue to create new
      }
    }

    // Create new connection if existing ones failed or none available
    const credentials = getJiraCredentials(jiraConnectionName);
    await this.selectCreateNewConnection();
    await this.fillNewConnectionForm(credentials);
    await this.waitForTestButtonEnabled();
    await this.testConnection();

    try {
      await this.page.getByText(/jira connection successful/i).waitFor({ state: 'visible', timeout: 15000 });
      await this.page.waitForFunction(() => {
        const rightFooter = document.querySelector('.jw-footer-right') ?? document.querySelector('.jw-footer');
        const nextBtn = rightFooter?.querySelector('button.jw-btn.jw-btn-primary');
        return nextBtn && !nextBtn.disabled;
      }, { timeout: 10000 });
      return 'new';
    } catch (error) {
      throw new Error(
        `Jira connection test failed for "${jiraConnectionName}". Verify .env credentials. Error: ${error.message}`
      );
    }
  }

 
}

export { JiraWebhookConnectJiraPage };