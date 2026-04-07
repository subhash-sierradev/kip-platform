// pages/JiraWebhookConnectJiraPage.js
import { BasePage } from '../Common_Files/BasePage.js';
import { getJiraCredentials } from '../../utils/JiraCredentials.js';

class JiraWebhookConnectJiraPage extends BasePage {
  constructor(page) {
    super(page);
    
    // Page-specific locators (common elements now in this.ui)
    this.connectionDropdown = page.locator('.cs-dropdown').filter({ hasText: 'Choose a saved connection' });
    this.verifyConnectionButton = page.getByRole('button', { name: 'Verify Connection' });
    this.testConnectionButton = page.getByRole('button', { name: 'Test Connection' });
    // New Connection form locators (placeholder text matches actual UI)
    this.jiraUrlInput = page.getByRole('textbox', { name: 'https://your-domain.atlassian.net' });
    this.connectionNameInput = page.getByRole('textbox', { name: 'Enter connection name' });
    this.emailAddressInput = page.getByRole('textbox', { name: 'your-email@example.com' });
    this.apiTokenInput = page.getByRole('textbox', { name: 'Enter API token (recommended) or password' });
    this.connectJiraHeading = page.getByText('Connect Your Jira Account');
    // Shows e.g. "1 connection • 1 active • 0 failed" — confirmed from DOM: div.cs-connection-meta-line
    this.connectionCountText = page.locator('.cs-connection-meta-line');
    // ── Smart Connection Locators ─────────────────────────────────────────
    // Used exclusively by manageJiraConnectionUpdated() — the smart connection orchestrator.
    // Scoped to the "Saved Connections" section for semantic anchoring:
    //   position-independent, works regardless of how many dropdowns are on the page,
    //   and text-change safe even after a connection is selected.
    this.savedConnectionsSection = page.locator('.cs-section', { hasText: 'Saved Connections' });
    this.scopedDropdown          = this.savedConnectionsSection.locator('.cs-dropdown');
    this.scopedDropdownList      = this.savedConnectionsSection.locator('.cs-dropdown-list');
  
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
      console.log('Error in selectConnection:', error.message);
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
      
      console.log('Dropdown opened, found connection elements:', count);
      
      // Close dropdown by pressing Escape
      await this.page.keyboard.press('Escape');
      
      return count > 0;
    } catch (error) {
      console.log('Error checking connections:', error.message);
      return false;
    }
  }

  async waitForVerifyButtonEnabled() {
    // Scoped to .cs-verification-row — confirmed from DOM (div.cs-verification-row > button.cs-btn)
    // Avoids scanning ALL page buttons — direct, fast, zero ambiguity
    await this.page.waitForFunction(() => {
      const verifyBtn = document.querySelector('.cs-verification-row button.cs-btn');
      return verifyBtn && !verifyBtn.disabled;
    }, { timeout: 10000 });
  }

  async waitForTestButtonEnabled() {
    await this.page.waitForFunction(() => {
      const buttons = Array.from(document.querySelectorAll('button'));
      const testButton = buttons.find(btn => btn.textContent && btn.textContent.includes('Test Connection'));
      return testButton && !testButton.disabled;
    }, { timeout: 10000 });
  }

  /**
   * Smart Jira connection orchestrator.
   * 1. If existing connections are available → select one and verify it.
   *    If verification succeeds → return 'existing'.
   *    If verification fails   → fall through to create new.
   * 2. Create a new connection using credentials from .env + a dynamic connection name.
   *    If test succeeds → return 'new'.
   *    Otherwise       → throw an error with a clear message.
   *
   * @param {string} jiraConnectionName - Dynamic name generated by GenerateTestData.generateJiraConnectionName()
   * @returns {'existing' | 'new'}
   */
  // ── ORIGINAL manageJiraConnection ────────────────────────────────────────
  // Alias so manageJiraConnection() can call it without breaking the original method name
  async hasActiveConnections() {
    return this.hasActiveConnectionsUpdated();
  }

  async manageJiraConnection(jiraConnectionName) {
    // Use hasActiveConnections() — checks both existence AND active status
    // Same logic as CreateJiraWebhookPage.connectionCountText check
    const hasActive = await this.hasActiveConnections();

    if (hasActive) {
      console.log('[manageJiraConnection] Active connections found. Trying existing connection...');
      try {
        await this.selectConnection();
        await this.waitForVerifyButtonEnabled();
        await this.verifyConnection();

        // Wait up to 15s for 'Verified' button — same pattern as CreateJiraWebhookPage
        await this.ui.buttons.verified.waitFor({ state: 'visible', timeout: 15000 });
        const nextReady = await this.isEnabled(this.ui.buttons.next);

        if (nextReady) {
          console.log('[manageJiraConnection] Existing connection verified successfully.');
          return 'existing';
        }
        console.log('[manageJiraConnection] Existing connection verification failed. Switching to new connection...');
      } catch (error) {
        console.log('[manageJiraConnection] Error verifying existing connection:', error.message);
        console.log('[manageJiraConnection] Switching to new connection...');
      }
    } else {
      console.log('[manageJiraConnection] No existing connections found. Creating new connection...');
    }

    // --- Create New Connection ---
    const credentials = getJiraCredentials(jiraConnectionName);
    await this.selectCreateNewConnection();
    await this.fillNewConnectionForm(credentials);
    await this.waitForTestButtonEnabled();
    await this.testConnection();

    // Wait up to 15s for 'Verified' button after test
    try {
      await this.ui.buttons.verified.waitFor({ state: 'visible', timeout: 15000 });
      const nextReady = await this.isEnabled(this.ui.buttons.next);
      if (nextReady) {
        console.log('[manageJiraConnection] New connection created and tested successfully:', jiraConnectionName);
        return 'new';
      }
    } catch (error) {
      // fall through to throw
    }

    throw new Error(
      `[manageJiraConnection] Jira connection test failed for "${jiraConnectionName}". ` +
      `Verify your .env credentials (JIRA_BASE_URL, JIRA_EMAIL, JIRA_API_TOKEN).`
    );
  }

  // ════════════════════════════════════════════════════════════════════════
  // ── SMART CONNECTION METHODS ─────────────────────────────────────────────
  // The methods below are the UPDATED versions used by the smart connection
  // orchestrator (manageJiraConnectionUpdated). They rely on the smart locators
  // defined at the end of the constructor (savedConnectionsSection,
  // scopedDropdown, scopedDropdownList) for semantic anchoring.
  // ════════════════════════════════════════════════════════════════════════

  // ── UPDATED — uses .cs-dropdown-list/.cs-connection-name instead of the legacy .cs-option locator
  // Used in: manageJiraConnectionUpdated()
  async selectConnectionUpdated(connectionName = null) {
    try {
      // Open the dropdown (existing locator works fine)
      await this.connectionDropdown.click();

      // Wait for the list container to appear
      const dropdownList = this.page.locator('.cs-dropdown-list');
      await dropdownList.waitFor({ state: 'visible', timeout: 5000 });

      if (connectionName) {
        // Click the item that contains the specific connection name text
        await dropdownList.locator('.cs-connection-name', { hasText: connectionName }).click();
      } else {
        // Click the first connection item — .cs-connection-name is the clickable row
        await dropdownList.locator('.cs-connection-name').first().click();
      }
    } catch (error) {
      console.log('[selectConnectionUpdated] Error:', error.message);
      throw error;
    }
  }

  // ── UPDATED — selects "Use Existing Connection" radio first ──────────────
  // Dropdown only appears when the existing radio is selected.
  // Original hasAvailableConnections() missed this step causing false negatives.
  // Used in: hasActiveConnectionsUpdated() → manageJiraConnectionUpdated()
  async hasAvailableConnectionsUpdated() {
    try {
      // Wait for the connection section to be visible first
      await this.connectJiraHeading.waitFor({ state: 'visible', timeout: 5000 });

      // MUST select "Use Existing Connection" radio first —
      // the dropdown only appears when this radio is selected
      await this.click(this.ui.formElements.useExistingConnection);

      // Wait for dropdown to appear after radio selection
      await this.connectionDropdown.waitFor({ state: 'visible', timeout: 5000 });

      // Click dropdown to open it
      await this.connectionDropdown.click();

      // Wait for dropdown options to appear
      await this.page.locator('.cs-dropdown-list, .cs-option').first().waitFor({ state: 'visible', timeout: 3000 });

      // Check for connection elements in the opened dropdown
      const connectionElements = this.page.locator('.cs-connection-name');
      const count = await connectionElements.count();

      console.log('[Updated] Dropdown opened, found connection elements:', count);

      // Close dropdown by pressing Escape
      await this.page.keyboard.press('Escape');

      return count > 0;
    } catch (error) {
      console.log('[Updated] Error checking connections:', error.message);
      return false;
    }
  }

  /**
   * UPDATED — Checks if at least one ACTIVE connection exists.
   * Uses hasAvailableConnectionsUpdated() which correctly selects the radio first.
   * Reads the connection count text (e.g. "3 connections: 2 active, 1 failed").
   * Used in: manageJiraConnectionUpdated()
   * @returns {Promise<boolean>}
   */
  async hasActiveConnectionsUpdated() {
    try {
      // Use updated version — correctly selects radio before checking dropdown
      const hasAny = await this.hasAvailableConnectionsUpdated();
      if (!hasAny) {
        console.log('[hasActiveConnectionsUpdated] No connections found in dropdown.');
        return false;
      }

      // Now check the active count from the status text
      const countText = await this.connectionCountText.textContent({ timeout: 3000 }).catch(() => null);
      if (!countText) {
        // Status text not visible — assume connections may be active
        console.log('[hasActiveConnectionsUpdated] Connection count text not found, assuming active.');
        return true;
      }

      const isZeroActive = countText.includes('0 active');
      console.log(`[hasActiveConnectionsUpdated] Count text: "${countText}" → active: ${!isZeroActive}`);
      return !isZeroActive;
    } catch (error) {
      console.log('[hasActiveConnectionsUpdated] Error:', error.message);
      return false;
    }
  }

  /**
   * UPDATED — Smart Jira connection orchestrator.
   * Smart flow:
   *   Case 1 → Active connections exist → try each one → first that verifies → return 'existing'
   *   Case 2 → No active connections   → create new → test → return 'new'
   *   Case 3 → All active connections fail verify → create new → test → return 'new'
   *
   * Fixes over manageJiraConnection():
   *   [Fix 1] this.scopedDropdown scoped to "Saved Connections" section via this.savedConnectionsSection
   *           — position-independent, text-change safe after a connection is selected
   *   [Fix 2] Removed waitForTimeout(500) — waitForVerifyButtonEnabled() polling is enough
   *   [Fix 3] Loop timeout reduced to 8000ms — avoids 15s × N waste on failed connections
   *   [Fix 4] New connection path: waitForFunction polls Next button before returning 'new'
   *   [Fix 5] this.scopedDropdownList scoped inside savedConnectionsSection — not page-wide
   *
   * Uses smart locators from constructor:
   *   this.savedConnectionsSection, this.scopedDropdown, this.scopedDropdownList
   *
   * @param {string} jiraConnectionName - Dynamic name generated by GenerateTestData.generateJiraConnectionName()
   * @returns {'existing' | 'new'}
   */
  async manageJiraConnectionUpdated(jiraConnectionName) {
    const activeOptionSelector = '.cs-option:has(.cs-badge-success)';

    // STEP 1: Select "Use an Existing Connection" radio so the dropdown + list appears
    await this.click(this.ui.formElements.useExistingConnection);
    console.log('[manageJiraConnectionUpdated] Step 1: Selected "Use an Existing Connection".');

    // STEP 2: Open the dropdown ONCE — count active connections while it is open
    // Uses this.scopedDropdown — scoped to savedConnectionsSection [Fix 1 + Fix 5]
    await this.scopedDropdown.waitFor({ state: 'visible', timeout: 5000 });
    await this.scopedDropdown.click();
    console.log('[manageJiraConnectionUpdated] Step 2: Opened connection dropdown.');

    // STEP 3: Count ONLY active connections (.cs-badge-success = Active badge)
    let activeCount = 0;
    try {
      await this.scopedDropdownList.waitFor({ state: 'visible', timeout: 5000 });
      activeCount = await this.scopedDropdownList.locator(activeOptionSelector).count();
      console.log(`[manageJiraConnectionUpdated] Active connections in dropdown: ${activeCount}`);
    } catch {
      console.log('[manageJiraConnectionUpdated] Dropdown list did not appear — no saved connections.');
    }

    if (activeCount === 0) {
      // No active connections — close dropdown, go straight to Create New
      await this.page.keyboard.press('Escape');
      console.log('[manageJiraConnectionUpdated] No active connections found → Creating new connection...');
    }

    // STEP 4: Try each ACTIVE connection — pick first one that verifies successfully
    // KEY: For i=0, dropdown is ALREADY OPEN — click directly (no reopen needed).
    //      For i>0, reopen using this.scopedDropdown — text-change safe [Fix 1].
    for (let i = 0; i < activeCount; i++) {
      console.log(`[manageJiraConnectionUpdated] Trying active connection ${i + 1} of ${activeCount}...`);
      try {
        if (i > 0) {
          // [Fix 1] this.scopedDropdown — safe even after text changes from prior selection
          await this.click(this.ui.formElements.useExistingConnection);
          await this.scopedDropdown.waitFor({ state: 'visible', timeout: 5000 });
          await this.scopedDropdown.click();
          await this.scopedDropdownList.waitFor({ state: 'visible', timeout: 8000 });
        }
        // i=0: dropdown already open from Step 2 — click directly

        // Click the i-th active connection
        const activeOption = this.scopedDropdownList.locator(activeOptionSelector).nth(i);
        await activeOption.waitFor({ state: 'visible', timeout: 5000 });
        await activeOption.click();
        console.log(`[manageJiraConnectionUpdated] Clicked active connection at index ${i}.`);

        // [Fix 2] No waitForTimeout(500) — waitForVerifyButtonEnabled() polls until ready
        await this.waitForVerifyButtonEnabled();
        await this.verifyConnection();

        // [Fix 3] 8000ms timeout in loop — avoids 15s × N waste on consistently failing connections
        await this.ui.buttons.verified.waitFor({ state: 'visible', timeout: 8000 });

        // Poll until Next button inside footer is enabled.
        // Primary scope: .jw-footer-right (right side of footer).
        // Fallback: .jw-footer (full footer) — confirmed from DOM: footer.jw-footer
        // Race condition safe — retries until enabled or 5s timeout.
        await this.page.waitForFunction(() => {
          const rightFooter = document.querySelector('.jw-footer-right') ?? document.querySelector('.jw-footer');
          const nextBtn = rightFooter?.querySelector('button.jw-btn.jw-btn-primary');
          return nextBtn && !nextBtn.disabled;
        }, { timeout: 5000 });

        console.log(`[manageJiraConnectionUpdated] Active connection ${i + 1} verified ✓ → using existing.`);
        return 'existing';
      } catch (err) {
        console.log(`[manageJiraConnectionUpdated] Active connection ${i + 1} error: ${err.message}. Trying next...`);
      }
    }

    // Reach here only if: 0 active connections OR all active ones failed verify
    if (activeCount > 0) {
      console.log('[manageJiraConnectionUpdated] All active connections failed → Creating new connection...');
    }

    // STEP 5: Create a brand-new connection
    const credentials = getJiraCredentials(jiraConnectionName);
    await this.selectCreateNewConnection();
    await this.fillNewConnectionForm(credentials);
    await this.waitForTestButtonEnabled();
    await this.testConnection();

    // [Fix 4] Wait for success text AND poll until Next is enabled before returning 'new'
    // Success text and Next button enable are NOT simultaneous — polling guarantees both
    try {
      await this.page.getByText(/jira connection successful/i).waitFor({ state: 'visible', timeout: 15000 });
      // Poll until Next button inside footer is enabled.
      // Primary scope: .jw-footer-right (right side of footer).
      // Fallback: .jw-footer (full footer) — confirmed from DOM: footer.jw-footer
      await this.page.waitForFunction(() => {
        const rightFooter = document.querySelector('.jw-footer-right') ?? document.querySelector('.jw-footer');
        const nextBtn = rightFooter?.querySelector('button.jw-btn.jw-btn-primary');
        return nextBtn && !nextBtn.disabled;
      }, { timeout: 10000 });
      console.log('[manageJiraConnectionUpdated] New connection tested successfully:', jiraConnectionName);
      return 'new';
    } catch (error) {
      throw new Error(
        `[manageJiraConnectionUpdated] Jira connection test failed for "${jiraConnectionName}". ` +
        `Verify .env credentials (JIRA_BASE_URL, JIRA_EMAIL, JIRA_API_TOKEN). Error: ${error.message}`
      );
    }
  }

 
}

export { JiraWebhookConnectJiraPage };