import { expect } from '@playwright/test';
class JiraWebhookViewPage {
  constructor(page) {
    this.page = page;
    
    // Page navigation verification
    this.breadcrumbText = page.getByText('Jira Webhook Details');
    
    // Navigation and tabs - using the actual DOM structure from page snapshot
    this.webhookDetailsTab = page.getByRole('tab', { name: 'Webhook Details' });
    this.samplePayloadTab = page.getByRole('tab', { name: 'Sample Payload' });
    this.jiraFieldMappingTab = page.getByRole('tab', { name: 'Jira Field Mapping' });
    this.webhookHistoryTab = page.getByRole('tab', { name: 'Webhook History' });
    
    // Webhook Details section
    this.webhookNameAlt = page.getByText('WEBHOOK NAME').locator('xpath=following-sibling::*[1]');
    this.webhookDescriptionAlt = page.getByText('DESCRIPTION').locator('xpath=following-sibling::*[1]');
    this.webhookUrlAlt = page.getByText('WEBHOOK URL').locator('xpath=following-sibling::*[1]');
    
    // Field mapping verification selectors
    this.projectField = page.locator('tr', { has: page.locator('span.field-name', { hasText: 'Project' }) }).locator('span.mapped-value');
    this.issueTypeField = page.locator('tr', { has: page.locator('span.field-name', { hasText: 'Issue Type' }) }).locator('span.mapped-value');
    this.summaryField = page.locator('tr', { has: page.locator('span.field-name', { hasText: 'Summary' }) }).locator('span.mapped-value');
    this.descriptionField = page.locator('tr', { has: page.locator('span.field-name', { hasText: 'Description' }) }).locator('span.mapped-value');

    // Page title
    this.pageTitle = page.locator('h1');
    
    // Back navigation
    this.backToListButton = page.locator('button:has-text("← Back"), .back-button, [aria-label="Back"]').first();
    
    // Management buttons
    this.testWebhookButton = page.locator('button:has-text("Test Webhook")').first();
    this.editWebhookButton = page.locator('button:has-text("Edit Webhook")').first();
    this.cloneWebhookButton = page.locator('button:has-text("Clone Webhook")').first();
    this.deleteWebhookButton = page.locator('button:has-text("Delete Webhook")').first();
  }

  // Navigate to specific tabs
  async clickWebhookDetailsTab() {
    // Wait for tab to be visible before clicking
    await expect(this.webhookDetailsTab).toBeVisible({ timeout: 10000 });
    await this.webhookDetailsTab.click();
    await this.page.waitForLoadState('domcontentloaded'); // Wait for tab content to load
  }

  async clickSamplePayloadTab() {
    await expect(this.samplePayloadTab).toBeVisible({ timeout: 10000 });
    await this.samplePayloadTab.click();
    await this.page.waitForLoadState('domcontentloaded');
  }

  async clickJiraFieldMappingTab() {
    await expect(this.jiraFieldMappingTab).toBeVisible({ timeout: 10000 });
    await this.jiraFieldMappingTab.click();
    await this.page.waitForLoadState('domcontentloaded');
  }

  async clickWebhookHistoryTab() {
    await expect(this.webhookHistoryTab).toBeVisible({ timeout: 10000 });
    await this.webhookHistoryTab.click();
    await this.page.waitForLoadState('domcontentloaded');
  }

  // Get webhook basic details
  async getWebhookName() {
    try {
      // Use the label-based selector that matches actual DOM structure
      await expect(this.webhookNameAlt, 'Webhook Name value not found next to label')
        .toBeVisible({ timeout: 5000 });
      return (await this.webhookNameAlt.textContent()).trim();
    } catch (error) {
      console.error('Failed to extract webhook name:', error.message);
      return null; // Return null to indicate extraction failure
    }
  }

  async getWebhookDescription() {
    try {
      // Use the label-based selector that matches actual DOM structure
      await expect(this.webhookDescriptionAlt, 'Webhook Description value not found next to label')
        .toBeVisible({ timeout: 5000 });
      return (await this.webhookDescriptionAlt.textContent()).trim();
    } catch (error) {
      console.error('Failed to extract webhook description:', error.message);
      return null; // Return null to indicate extraction failure
    }
  }

  async getWebhookUrl() {
    try {
      // Use the label-based selector that matches actual DOM structure
      await expect(this.webhookUrlAlt).toBeVisible({ timeout: 3000 });
      return (await this.webhookUrlAlt.textContent()).trim();
    } catch (error) {
      console.error('Failed to extract webhook URL:', error.message);
      return null; // Return null to indicate extraction failure
    }
  }


  // Verify webhook basic details match expected data
  async verifyWebhookBasicDetails(expectedData) {
    const actualName = await this.getWebhookName();
    const actualDescription = await this.getWebhookDescription();
    const actualUrl = await this.getWebhookUrl();

    // Handle null values (extraction failures)
    const actualNameTrimmed = actualName ? actualName.trim() : null;
    const actualDescriptionTrimmed = actualDescription ? actualDescription.trim() : null;
    
    return {
      nameMatches: actualNameTrimmed === expectedData.webhookName,
      descriptionMatches: actualDescriptionTrimmed === expectedData.description,
      urlExists: actualUrl && actualUrl.trim().length > 0,
      actualName: actualNameTrimmed,
      actualDescription: actualDescriptionTrimmed,
      actualUrl: actualUrl ? actualUrl.trim() : null,
    };
  }

  // Get field mapping values
  async getProjectMapping() {
    try {
      await expect(this.projectField).toBeVisible({ timeout: 5000 });
      return (await this.projectField.textContent()).trim();
    } catch (error) {
      console.error('Failed to extract project mapping:', error.message);
      return null;
    }
  }

  async getIssueTypeMapping() {
    try {
      await expect(this.issueTypeField).toBeVisible({ timeout: 5000 });
      return (await this.issueTypeField.textContent()).trim();
    } catch (error) {
      console.error('Failed to extract issue type mapping:', error.message);
      return null;
    }
  }

  async getSummaryMapping() {
    try {
      await expect(this.summaryField).toBeVisible({ timeout: 5000 });
      return (await this.summaryField.textContent()).trim();
    } catch (error) {
      console.error('Failed to extract summary mapping:', error.message);
      return null;
    }
  }

  async getDescriptionMapping() {
    try {
      await expect(this.descriptionField).toBeVisible({ timeout: 5000 });
      return (await this.descriptionField.textContent()).trim();
    } catch (error) {
      console.error('Failed to extract description mapping:', error.message);
      return null;
    }
  }

  // Verify field mappings match expected data
  async verifyFieldMappings(expectedData) {
    // First ensure we're on the field mapping tab
    await this.clickJiraFieldMappingTab();
    
    // Add a small wait to ensure content loads
    await this.page.waitForLoadState('domcontentloaded');
    
    const actualProject = await this.getProjectMapping();
    const actualIssueType = await this.getIssueTypeMapping();
    const actualSummary = await this.getSummaryMapping();
    const actualDescription = await this.getDescriptionMapping();

    return {
      projectMatches: actualProject === null ? false : actualProject === expectedData.projectName,
      issueTypeMatches: actualIssueType === null ? false : actualIssueType === expectedData.issueType,
      summaryMatches: actualSummary === null ? false : actualSummary === expectedData.summaryTemplate,
      descriptionMatches: actualDescription === null ? false : actualDescription === expectedData.descriptionTemplate,
      actualProject: actualProject,
      actualIssueType: actualIssueType,
      actualSummary: actualSummary,
      actualDescription: actualDescription,
    };
  }

  // Navigation helpers
  async navigateBackToList() {
    await this.backToListButton.click();
    await this.page.waitForLoadState('domcontentloaded');
  }

  // Management actions
  async clickTestWebhook() {
    await this.testWebhookButton.click();
    await this.page.waitForLoadState('domcontentloaded');
  }

  async clickEditWebhook() {
    await this.editWebhookButton.click();
    await this.page.waitForLoadState('domcontentloaded');
  }

  async clickCloneWebhook() {
    await this.cloneWebhookButton.click();
    await this.page.waitForLoadState('domcontentloaded');
  }

  async clickDeleteWebhook() {
    await this.deleteWebhookButton.click();
    await this.page.waitForLoadState('domcontentloaded');
  }

  // Comprehensive verification method
  async verifyCompleteWebhookData(expectedData) {
    const results = {
      basicDetails: null,
      fieldMappings: null
    };

    // Verify basic details on webhook details tab
    await this.clickWebhookDetailsTab();
    results.basicDetails = await this.verifyWebhookBasicDetails({
      webhookName: expectedData.validWebhookName,
      description: expectedData.validDescription
    });

    // Verify field mappings on field mapping tab
    await this.clickJiraFieldMappingTab();
    results.fieldMappings = await this.verifyFieldMappings(expectedData);

    return results;
  }
}

export { JiraWebhookViewPage };