// pages/JiraWebhookSamplePayloadPage.js
import { BasePage } from '../Common_Files/BasePage.js';

class JiraWebhookSamplePayloadPage extends BasePage {
  constructor(page) {
    super(page);
    
    // Page-specific locators (common elements now in this.ui)
    this.jsonPayloadInput = page.getByRole('textbox', { name: 'Paste your JSON payload here' });
    this.uploadFileButton = page.getByRole('button', { name: '📁 Upload File' });
    this.fromClipboardButton = page.getByRole('button', { name: '📋 From Clipboard' });
    this.samplePayloadHeading = page.getByText('Sample Webhook Payload for Jira Field Mapping');
  }

  async fillJsonPayload(payload) {
    await this.fill(this.jsonPayloadInput, payload);
  }

  async formatJson() {
    await this.click(this.ui.buttons.formatJson);
  }

  async clickNext() {
    await this.click(this.ui.buttons.next);
  }

  async isFormatJsonEnabled() {
    return await this.isEnabled(this.ui.buttons.formatJson);
  }

  async isNextButtonEnabled() {
    return await this.isEnabled(this.ui.buttons.next);
  }
}

export { JiraWebhookSamplePayloadPage };