// pages/JiraWebhookFieldMappingPage.js
import { BasePage } from '../Common_Files/BasePage.js';

class JiraWebhookFieldMappingPage extends BasePage {
  constructor(page) {
    super(page);
    
    // Static locators - updated to match actual form structure
    this.projectDropdown = page.getByRole('combobox').nth(2);
    this.issueTypeDropdown = page.getByRole('combobox').nth(4);
    this.summaryTemplateInput = page.getByRole('textbox', { name: 'Enter issue summary template' });
    this.descriptionTemplateInput = page.getByRole('textbox', { name: 'Enter issue description' });
    this.nextButton = page.getByRole('button', { name: 'Next →' });
    this.incomingJsonHeading = page.getByText('Incoming JSON');
    this.mapToJiraFieldsHeading = page.getByText('Map to Jira Fields');
  }

  async selectProject(projectName) {
    await this.projectDropdown.selectOption(projectName);
  }

  async selectIssueType(issueType) {
    await this.issueTypeDropdown.selectOption(issueType);
  }

  async selectAssignee(assignee) {
    await this.assigneeDropdown.selectOption(assignee);
  }

  async fillSummaryTemplate(template) {
    await this.summaryTemplateInput.fill(template);
  }

  async fillDescriptionTemplate(template) {
    await this.descriptionTemplateInput.fill(template);
  }

  async clickNext() {
    await this.nextButton.click();
  }

  async isNextButtonEnabled() {
    return await this.nextButton.isEnabled();
  }
}

export { JiraWebhookFieldMappingPage };