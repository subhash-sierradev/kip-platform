// Common_Files/POManager.js - Page Object Manager
// Folder structure: Common_Files | Admin_Screens | Jira_Webhook | ArcGIS_Integration
import { LoginPage } from './LoginPage.js';
import { BasePage } from './BasePage.js';
import { UICommonElements } from './UICommonElements.js';
import { AdminNavigationPage } from '../Admin_Screens/AdminNavigationPage.js';
import { AdminAuditLogsPage } from '../Admin_Screens/AdminAuditLogsPage.js';
import { AdminJiraConnectPage } from '../Admin_Screens/AdminJiraConnectPage.js';
import { AdminArcGISConnectPage } from '../Admin_Screens/AdminArcGISConnectPage.js';
import { JiraWebhookCreatorPage } from '../Jira_Webhook/CreateJiraWebhookPage.js';
import { JiraWebhookEditPage } from '../Jira_Webhook/EditJiraWebhook.js';
import { JiraWebhookBasicDetailsPage } from '../Jira_Webhook/JiraWebhookBasicDetailsPage.js';
import { JiraWebhookConnectJiraPage } from '../Jira_Webhook/JiraWebhookConnectJiraPage.js';
import { JiraWebhookFieldMappingPage } from '../Jira_Webhook/JiraWebhookFieldMappingPage.js';
import { JiraWebhookManagementPage } from '../Jira_Webhook/JiraWebhookManagementPage.js';
import { JiraWebhookReviewCreatePage } from '../Jira_Webhook/JiraWebhookReviewCreatePage.js';
import { JiraWebhookSamplePayloadPage } from '../Jira_Webhook/JiraWebhookSamplePayloadPage.js';
import { JiraWebhookViewPage } from '../Jira_Webhook/JiraWebhookViewPage.js';
import { ArcGISIntegrationCreatorPage } from '../ArcGIS_Integration/ArcGISIntegrationCreatorPage.js';
import { ArcGISIntegrationManagementPage } from '../ArcGIS_Integration/ArcGISIntegrationManagementPage.js';
import { ArcGISIntegrationEditorPage } from '../ArcGIS_Integration/ArcGISIntegrationEditorPage.js';
import { ArcGISIntegrationViewPage } from '../ArcGIS_Integration/ArcGISIntegrationViewPage.js';
import { ConfluenceIntegrationCreatorPage } from '../Confluence_Integration/Confluence_Integration_Creator_Page.js';
import { ConfluenceIntegrationEditorPage } from '../Confluence_Integration/Confluence_Integration_Editor_Page.js';

class POManager {
  constructor(page) {
    this.page = page;
  }

  // Direct access to common UI elements
  get ui() {
    return this._ui = this._ui || new UICommonElements(this.page);
  }

  // Lazy-loaded page objects

  get loginPage() {
    return this._loginPage = this._loginPage || new LoginPage(this.page);
  }

  get basePage() {
    return this._basePage = this._basePage || new BasePage(this.page);
  }

  get adminNavigationPage() {
    return this._adminNavigationPage = this._adminNavigationPage || new AdminNavigationPage(this.page);
  }

  get adminAuditLogsPage() {
    return this._adminAuditLogsPage = this._adminAuditLogsPage || new AdminAuditLogsPage(this.page);
  }

  get adminJiraConnectPage() {
    return this._adminJiraConnectPage = this._adminJiraConnectPage || new AdminJiraConnectPage(this.page);
  }

  get adminArcGISConnectPage() {
    return this._adminArcGISConnectPage = this._adminArcGISConnectPage || new AdminArcGISConnectPage(this.page);
  }

  get createJiraWebhookPage() {
    return this._createJiraWebhookPage = this._createJiraWebhookPage || new JiraWebhookCreatorPage(this.page);
  }

  get editJiraWebhookPage() {
    return this._editJiraWebhookPage = this._editJiraWebhookPage || new JiraWebhookEditPage(this.page);
  }

  get jiraWebhookBasicDetailsPage() {
    return this._jiraWebhookBasicDetailsPage = this._jiraWebhookBasicDetailsPage || new JiraWebhookBasicDetailsPage(this.page);
  }

  get jiraWebhookConnectJiraPage() {
    return this._jiraWebhookConnectJiraPage = this._jiraWebhookConnectJiraPage || new JiraWebhookConnectJiraPage(this.page);
  }

  get jiraWebhookFieldMappingPage() {
    return this._jiraWebhookFieldMappingPage = this._jiraWebhookFieldMappingPage || new JiraWebhookFieldMappingPage(this.page);
  }

  get jiraWebhookManagementPage() {
    return this._jiraWebhookManagementPage = this._jiraWebhookManagementPage || new JiraWebhookManagementPage(this.page);
  }

  get jiraWebhookReviewCreatePage() {
    return this._jiraWebhookReviewCreatePage = this._jiraWebhookReviewCreatePage || new JiraWebhookReviewCreatePage(this.page);
  }

  get jiraWebhookSamplePayloadPage() {
    return this._jiraWebhookSamplePayloadPage = this._jiraWebhookSamplePayloadPage || new JiraWebhookSamplePayloadPage(this.page);
  }

  get jiraWebhookViewPage() {
    return this._jiraWebhookViewPage = this._jiraWebhookViewPage || new JiraWebhookViewPage(this.page);
  }

  // ArcGIS Integration page objects
  get arcgisIntegrationCreatorPage() {
    return this._arcgisIntegrationCreatorPage = this._arcgisIntegrationCreatorPage || new ArcGISIntegrationCreatorPage(this.page);
  }

  get arcgisIntegrationManagementPage() {
    return this._arcgisIntegrationManagementPage = this._arcgisIntegrationManagementPage || new ArcGISIntegrationManagementPage(this.page);
  }

  get arcgisIntegrationEditorPage() {
    return this._arcgisIntegrationEditorPage = this._arcgisIntegrationEditorPage || new ArcGISIntegrationEditorPage(this.page);
  }

  get arcgisIntegrationViewPage() {
    return this._arcgisIntegrationViewPage = this._arcgisIntegrationViewPage || new ArcGISIntegrationViewPage(this.page);
  }

  // Confluence Integration page objects
  get confluenceIntegrationCreatorPage() {
    return this._confluenceIntegrationCreatorPage = this._confluenceIntegrationCreatorPage || new ConfluenceIntegrationCreatorPage(this.page);
  }

  get confluenceIntegrationEditorPage() {
    return this._confluenceIntegrationEditorPage = this._confluenceIntegrationEditorPage || new ConfluenceIntegrationEditorPage(this.page);
  }
}

export { POManager };