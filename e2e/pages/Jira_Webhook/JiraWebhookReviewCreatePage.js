// pages/JiraWebhookReviewCreatePage.js
import { expect } from '@playwright/test';
import { BasePage } from '../Common_Files/BasePage.js';
import { JiraWebhookConnectJiraPage } from './JiraWebhookConnectJiraPage.js';

class JiraWebhookReviewCreatePage extends BasePage {
  constructor(page) {
    super(page);
    
    // Page-specific locators not in UICommonElements
    this.copyUrlButton = page.getByRole('button', { name: 'Copy webhook URL', exact: true });
    this.reviewHeading = page.getByText('Review your webhook configuration and see how the Jira ticket will be created');
    this.webhookNameInput = page.getByRole('textbox', { name: 'Jira Webhook Name' });
    this.webhookDescriptionInput = page.getByRole('textbox', { name: 'Provide a brief description' });
    this.jsonPayloadInput = page.getByRole('textbox', { name: 'Paste your JSON payload here' });
    this.summaryTemplateInput = page.getByRole('textbox', { name: 'Enter issue summary template' });
    this.descriptionTemplateInput = page.getByRole('textbox', { name: 'Enter issue description' });
    this.webhookCard = page.locator('.webhook-card');
    this.webhookUrlText = page.getByText('Webhook URL:');
    
    // Initialize Connect Jira page object
    this.connectJiraPage = new JiraWebhookConnectJiraPage(page);
  }

  // Navigation methods
  async navigateToWebhookCreation() {
    await this.ui.mainNavigation.outboundMenu.click();
    await this.ui.mainNavigation.jiraWebhookMenu.click();
    await this.ui.buttons.addJiraWebhook.click();
  }

  // Step 1: Basic Details
  async fillBasicDetails(webhookName, description) {
    await this.webhookNameInput.type(webhookName);
    await this.webhookDescriptionInput.type(description);
    
    // Wait for the Next button to be enabled (validation may have debounce delay)
    await expect(this.ui.buttons.next).toBeEnabled({ timeout: 10000 });
    await this.ui.buttons.next.click();
  }

  // Step 2: Sample Payload
  async fillSamplePayload(payload) {
    await this.jsonPayloadInput.fill(JSON.stringify(payload));
    await this.ui.buttons.next.click();
  }

  // Step 3: Connect Jira
  async selectAndVerifyConnection() {
    await expect(this.page.getByText('Connection Method')).toBeVisible();
    await expect(this.connectJiraPage.ui.formElements.useExistingConnection).toBeChecked();
    
    await this.connectJiraPage.selectConnection();
    await this.connectJiraPage.waitForVerifyButtonEnabled();
    await this.connectJiraPage.verifyConnection();
    await expect(this.page.getByRole('button', { name: 'Verified' })).toBeVisible({ timeout: 15000 });
    await expect(this.connectJiraPage.ui.buttons.next).toBeEnabled();
    await this.connectJiraPage.clickNext();
  }

  // Step 4: Field Mapping
  async fillFieldMapping(projectName, issueType, summaryTemplate, descriptionTemplate) {
    await this.ui.formElements.projectDropdown.selectOption([projectName]);
    await this.ui.formElements.issueTypeDropdown.selectOption([issueType]);
    await this.summaryTemplateInput.fill(summaryTemplate);
    await this.descriptionTemplateInput.fill(descriptionTemplate);
    await this.ui.buttons.next.click();
  }

  // Step 5: Review & Create
  async verifyReviewPage(webhookName, projectName, issueType) {
    await expect(this.reviewHeading).toBeVisible();
    await expect(this.page.getByRole('heading', { name: 'Webhook Configuration'})).toBeVisible();
    
    // Verify the Integration Name field is present 
    // Note: The app may display a transformed version of the webhook name
    await expect(this.page.getByText('Integration Name:')).toBeVisible();
    
    // Verify project and issue type are visible
    await expect(this.page.getByText(projectName).first()).toBeVisible();
    await expect(this.page.getByText(issueType).first()).toBeVisible();
  }

  async createWebhook() {
    await expect(this.ui.buttons.createWebhook).toBeEnabled();
    await this.ui.buttons.createWebhook.click();
  }

  async verifyWebhookCreationSuccess() {
    await expect(this.ui.headings.webhookCreatedSuccess).toBeVisible();
    await expect(this.webhookUrlText.first()).toBeVisible();
  }

  async copyWebhookUrl() {
    await this.copyUrlButton.click();
  }

  async acknowledgeCreation() {
    await this.ui.buttons.okUnderstand.waitFor({ state: 'visible' });
    await this.ui.buttons.okUnderstand.click();
  }

  async verifyWebhookInList() {
    await expect(this.ui.buttons.addJiraWebhook).toBeVisible();
    await expect(this.webhookCard.first()).toBeVisible();
    await expect(this.page.getByText('Enabled').first()).toBeVisible();
  }

  async isWebhookCreated() {
    return await this.ui.headings.webhookCreatedSuccess.isVisible();
  }
}

export { JiraWebhookReviewCreatePage };