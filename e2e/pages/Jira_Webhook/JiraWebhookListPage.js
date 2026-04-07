// pages/JiraWebhookListPage.js
import { BasePage } from '../Common_Files/BasePage.js';

class JiraWebhookListPage extends BasePage {
  constructor(page) {
    super(page);
    // All UI elements now accessible via this.ui from UICommonElements
  }

  async navigateToJiraWebhooks() {
    await this.ui.mainNavigation.outboundMenu.click();
    await this.ui.mainNavigation.jiraWebhookMenu.click();
  }

  async startWebhookCreation() {
    await this.ui.buttons.addJiraWebhook.click();
  }
}

export { JiraWebhookListPage };