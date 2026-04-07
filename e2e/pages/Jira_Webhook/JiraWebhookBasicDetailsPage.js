// pages/JiraWebhookBasicDetailsPage.js
import { BasePage } from '../Common_Files/BasePage.js';

class JiraWebhookBasicDetailsPage extends BasePage {
  constructor(page) {
    super(page);
    
    // Page-specific locators only (common elements now in this.ui)
    this.webhookNameInput = page.getByRole('textbox', { name: 'Jira Webhook Name' });
    this.descriptionInput = page.getByRole('textbox', { name: 'Provide a brief description' });
    this.characterCount = page.locator('[class*="character-count"]');
  }

  async fillWebhookName(name) {
    await this.type(this.webhookNameInput, name);
  }

  async fillDescription(description) {
    await this.fill(this.descriptionInput, description);
  }

  async clickNext() {
    await this.click(this.ui.buttons.next);
  }

  async clickPrevious() {
    await this.click(this.ui.buttons.previous);
  }

  async isNextButtonEnabled() {
    return await this.isEnabled(this.ui.buttons.next);
  }

  async isNextButtonDisabled() {
    return await this.isDisabled(this.ui.buttons.next);
  }

  async getCharacterCount() {
    return await this.getText(this.ui.getCharacterCount());
  }

  // Navigation using common elements
  async navigateToJiraWebhook() {
    await this.click(this.ui.mainNavigation.outboundMenu);
    await this.click(this.ui.mainNavigation.jiraWebhookMenu);
  }

  // Verify page using common headings
  async verifyBasicDetailsPage() {
    return await this.isVisible(this.ui.headings.basicDetails);
  }

  async verifyCreateWebhookHeading() {
    return await this.isVisible(this.ui.headings.createJiraWebhook);
  }
}

export { JiraWebhookBasicDetailsPage };