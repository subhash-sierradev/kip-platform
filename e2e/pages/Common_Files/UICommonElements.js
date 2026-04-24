class UICommonElements {
  constructor(page) {
    this.page = page;
  }

  // ===========================================
  // NAVIGATION ELEMENTS
  // ===========================================
  
  // Main Navigation
  get mainNavigation() {
    return {
      outboundMenu: this.page.locator('div').filter({ hasText: /^Outbound$/ }),
      jiraWebhookMenu: this.page.locator('div').filter({ hasText: /^Jira Webhook$/ }),
      arcgisMenu: this.page.locator('div').filter({ hasText: /^ArcGIS Integration$/ }),
      confluenceMenu: this.page.locator('div').filter({ hasText: /^Confluence Integration$/ }),
    };
  }
  // Admin Navigation
  get adminNavigation() {
    return {
      // Admin menu button (left sidebar)
      adminMenuButton: this.page.locator('[data-testid="admin-menu"], .admin-menu, nav').getByText('Admin'),
      adminMenuSelector: this.page.locator('.nav-item[data-menu-key="admin"]'),
      
      // Admin submenu items
      clearCacheMenuItem: this.page.getByText('Clear Cache'),
      cacheStatisticsMenuItem: this.page.getByText('Cache Statistics'),
      siteConfigMenuItem: this.page.locator('.submenu-item, .menu-item').getByText('Site Config', { exact: true }),
    
      
      // Breadcrumb elements
      homeBreadcrumb: this.page.getByText('Home'),
      adminBreadcrumb: this.page.getByText('Admin'),
      breadcrumbText: this.page.getByRole('navigation', { name: 'Breadcrumb' }).locator('.breadcrumb-text')
    };
  }

  // Admin Navigation
  get navigation() {
    return {
      adminMenu: this.page.locator('div').filter({ hasText: /^Admin$/ }),
      auditLogsMenu: this.page.locator('div').filter({ hasText: /^Audit Logs$/ }),
      jiraConnectMenu: this.page.locator('div').filter({ hasText: /^Jira Connect$/ }),
      arcgisConnectMenu: this.page.locator('div').filter({ hasText: /^ArcGIS Connect$/ })
    };
  }

  // User Profile & Authentication
  get userProfile() {
    return {
      profileMenu: this.page.locator('div').filter({ hasText: /^Org2 Admin$/ }).nth(1),
      logoutOption: this.page.locator('div').filter({ hasText: /^Logout$/ }),
      welcomeText: this.page.getByText('Welcome to Kaseware Integration Platform')
    };
  }

  // ===========================================
  // COMMON BUTTONS
  // ===========================================
  
  get buttons() {
    return {
      // Navigation buttons
      next: this.page.getByRole('button', { name: 'Next →' }),
      previous: this.page.getByRole('button', { name: '← Previous' }),
      
      // Action buttons
      create: this.page.getByRole('button', { name: 'Create' }),
      createWebhook: this.page.getByRole('button', { name: 'Create Webhook' }),
      addJiraWebhook: this.page.getByRole('button', { name: '+ Add Jira Webhook' }),
      save: this.page.getByRole('button', { name: 'Save' }),
      cancel: this.page.getByRole('button', { name: 'Cancel' }),
      delete: this.page.getByRole('button', { name: 'Delete' }),
      edit: this.page.getByRole('button', { name: 'Edit' }),
      
      // View buttons
      gridView: this.page.getByRole('button', { name: 'Grid View' }),
      listView: this.page.getByRole('button', { name: 'List View' }),
      
      // Utility buttons
      formatJson: this.page.getByRole('button', { name: '✨ Format JSON' }),
      copyUrl: this.page.getByRole('button', { name: 'Copy Webhook URL' }),
      verified: this.page.getByRole('button', { name: 'Verified' }),
      
      // Modal buttons
      okUnderstand: this.page.getByRole('button', { name: 'OK, I Understand' }),
      confirm: this.page.getByRole('button', { name: 'Confirm' }),
      close: this.page.getByRole('button', { name: 'Close' })
    };
  }

  // ===========================================
  // FORM ELEMENTS
  // ===========================================
  
  get formElements() {
    return {
      // Common input fields
      searchBox: this.page.getByRole('textbox', { name: /Search.*/ }),
      nameInput: this.page.getByRole('textbox', { name: /.*[Nn]ame.*/ }),
      descriptionInput: this.page.getByRole('textbox', { name: /.*[Dd]escription.*/ }),
      
      // Dropdowns
      sortDropdown: this.page.getByRole('combobox', { name: 'Sort items by field' }),
      pageSizeDropdown: this.page.getByRole('combobox', { name: 'Select page size' }),
      projectDropdown: this.page.getByRole('combobox').nth(2),
      issueTypeDropdown: this.page.getByRole('combobox').nth(3),
      
      // Authentication specific
      usernameInput: this.page.getByRole('textbox', { name: 'Username or email' }),
      passwordInput: this.page.getByRole('textbox', { name: 'Password' }),
      signInButton: this.page.getByRole('button', { name: 'Sign In' }),
      
      // Radio buttons
      useExistingConnection: this.page.getByRole('radio', { name: 'Use an Existing Connection – Reuse a previously configured connection' }),
      createNewConnection: this.page.getByRole('radio', { name: 'Create a New Connection – Set up a new connection configuration' })
    };
  }

  // ===========================================
  // COMMON HEADINGS & TEXT
  // ===========================================
  
  get headings() {
    return {
      // Page headings
      createJiraWebhook: this.page.getByRole('heading', { name: 'Create Jira Webhook' }),
      basicDetails: this.page.getByText('Basic Details'),
      samplePayload: this.page.getByText('Sample Payload'),
      connectJira: this.page.getByText('Connect Jira'),
      fieldMapping: this.page.getByText('Field Mapping'),
      reviewCreate: this.page.getByText('Review & Create'),
      
      // Authentication
      signInHeading: this.page.getByRole('heading', { name: 'Sign in to your account' }),
      
      // Success messages
      webhookCreatedSuccess: this.page.getByRole('heading', { name: 'Webhook Created Successfully' })
    };
  }

  // ===========================================
  // STATUS INDICATORS
  // ===========================================
  
  get statusIndicators() {
    return {
      checkmark: this.page.getByText('✔'),
      loading: this.page.getByText('Loading...'),
      error: this.page.getByText('Error'),
      success: this.page.getByText('Success'),
      verified: this.page.getByText('Verified'),
      active: this.page.getByText('Active'),
      inactive: this.page.getByText('Inactive'),
      failed: this.page.getByText('Failed')
    };
  }

  // ===========================================
  // PAGINATION & FILTERING
  // ===========================================
  
  get pagination() {
    return {
      perPageText: this.page.getByText('Per Page:'),
      pageSizeSelect: this.page.getByRole('combobox', { name: 'Select page size' }),
      nextPage: this.page.getByRole('button', { name: 'Next page' }),
      previousPage: this.page.getByRole('button', { name: 'Previous page' }),
      firstPage: this.page.getByRole('button', { name: 'First page' }),
      lastPage: this.page.getByRole('button', { name: 'Last page' })
    };
  }

  // ===========================================
  // MODALS & DIALOGS
  // ===========================================
  
  get modals() {
    return {
      confirmDialog: this.page.getByRole('dialog'),
      modalOverlay: this.page.locator('.modal-overlay'),
      modalClose: this.page.getByRole('button', { name: 'Close' }),
      modalTitle: this.page.locator('.modal-title'),
      modalContent: this.page.locator('.modal-content')
    };
  }

  // ===========================================
  // NOTIFICATIONS & ALERTS
  // ===========================================
  
  get notifications() {
    return {
      successNotification: this.page.locator('[class*="success"]'),
      errorNotification: this.page.locator('[class*="error"]'),
      warningNotification: this.page.locator('[class*="warning"]'),
      infoNotification: this.page.locator('[class*="info"]'),
      toast: this.page.locator('[class*="toast"]')
    };
  }

  // ===========================================
  // TABLES & LISTS
  // ===========================================
  
  get tables() {
    return {
      table: this.page.getByRole('table'),
      tableHeader: this.page.getByRole('columnheader'),
      tableRow: this.page.getByRole('row'),
      tableCell: this.page.getByRole('cell'),
      tableBody: this.page.locator('tbody'),
      
      // List view elements
      listItem: this.page.getByRole('listitem'),
      listContainer: this.page.getByRole('list')
    };
  }

  // ===========================================
  // BREADCRUMBS & NAVIGATION HELPERS
  // ===========================================
  
  get breadcrumbs() {
    return {
      breadcrumbContainer: this.page.getByRole('navigation', { name: 'breadcrumb' }),
      breadcrumbLink: this.page.getByRole('link'),
      currentPage: this.page.locator('[aria-current="page"]')
    };
  }

  // ===========================================
  // HELPER METHODS FOR COMMON PATTERNS
  // ===========================================
  
  // Get any button by partial text
  getButtonByText(text) {
    return this.page.getByRole('button', { name: new RegExp(text, 'i') });
  }

  // Get any heading by partial text
  getHeadingByText(text) {
    return this.page.getByRole('heading', { name: new RegExp(text, 'i') });
  }

  // Get any input by partial label
  getInputByLabel(label) {
    return this.page.getByRole('textbox', { name: new RegExp(label, 'i') });
  }

  // Get any dropdown by partial label
  getDropdownByLabel(label) {
    return this.page.getByRole('combobox', { name: new RegExp(label, 'i') });
  }

  // Get step indicator in wizard flows
  getWizardStep(stepName) {
    return this.page.getByText(stepName);
  }

  // Get tab by name
  getTab(tabName) {
    return this.page.getByRole('tab', { name: new RegExp(tabName, 'i') });
  }

  // Get any link by text
  getLinkByText(text) {
    return this.page.getByRole('link', { name: new RegExp(text, 'i') });
  }

  // Character count indicators (common in your forms)
  getCharacterCount() {
    return this.page.getByText(/\d+\/\d+/);
  }

  // Connection status indicators
  getConnectionStatus() {
    return this.page.getByText(/connections.*active.*failed/);
  }
}

export { UICommonElements };