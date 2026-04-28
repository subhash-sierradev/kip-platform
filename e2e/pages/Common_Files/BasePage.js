import { UICommonElements } from './UICommonElements.js';

class BasePage {
  constructor(page) {
    this.page = page;
    this.timeout = 30000;
    this.ui = new UICommonElements(page); // Common UI elements available to all pages
  }

  // Essential interaction methods
  async click(selector, options = {}) {
    const locator = typeof selector === 'string' ? this.page.locator(selector) : selector;
    await locator.waitFor({ state: 'visible', timeout: this.timeout });
    await locator.click(options);
  }

  async fill(selector, text, options = {}) {
    const locator = typeof selector === 'string' ? this.page.locator(selector) : selector;
    await locator.waitFor({ state: 'visible', timeout: this.timeout });
    
    // Clear the field before filling to ensure reliable input
    await locator.click();
    await locator.clear();
    await locator.fill(text, options);
  }

  async selectOption(selector, value, options = {}) {
    const locator = typeof selector === 'string' ? this.page.locator(selector) : selector;
    await locator.waitFor({ state: 'visible', timeout: this.timeout });
    await locator.selectOption(value, options);
  }

  async check(selector, options = {}) {
    const locator = typeof selector === 'string' ? this.page.locator(selector) : selector;
    await locator.waitFor({ state: 'visible', timeout: this.timeout });
    await locator.check(options);
  }

  async uncheck(selector, options = {}) {
    const locator = this.page.locator(selector);
    await locator.waitFor({ state: 'visible', timeout: this.timeout });
    await locator.uncheck(options);
  }

  // Essential state checking methods
  async isVisible(selector, timeout = 5000) {
    try {
      const locator = typeof selector === 'string' ? this.page.locator(selector) : selector;
      return await locator.isVisible();
    } catch {
      return false;
    }
  }

  async isEnabled(selector) {
    const locator = typeof selector === 'string' ? this.page.locator(selector) : selector;
    await locator.waitFor({ state: 'visible', timeout: this.timeout });
    return await locator.isEnabled();
  }

  async isDisabled(selector) {
    const locator = this.page.locator(selector);
    await locator.waitFor({ state: 'visible', timeout: this.timeout });
    return await locator.isDisabled();
  }

  async isChecked(selector) {
    const locator = this.page.locator(selector);
    await locator.waitFor({ state: 'visible', timeout: this.timeout });
    return await locator.isChecked();
  }

  // Essential text retrieval methods
  async getText(selector, options = {}) {
    const locator = this.page.locator(selector);
    await locator.waitFor({ state: 'visible', timeout: this.timeout });
    return await locator.textContent(options);
  }

  // Essential navigation methods
  async goto(url, options = {}) {
    const defaultOptions = {
      waitUntil: 'domcontentloaded',
      timeout: this.timeout
    };
    await this.page.goto(url, { ...defaultOptions, ...options });
  }

  async getCurrentUrl() {
    return this.page.url();
  }

  // Essential waiting methods
  async waitForSelector(selector, options = {}) {
    const defaultOptions = {
      state: 'visible',
      timeout: this.timeout
    };
    return await this.page.locator(selector).waitFor({ ...defaultOptions, ...options });
  }

  async waitForText(text, timeout = this.timeout) {
    await this.page.getByText(text).waitFor({ 
      state: 'visible', 
      timeout 
    });
  }

  async waitForLoadState(state = 'domcontentloaded') {
    await this.page.waitForLoadState(state);
  }

  // Essential utility methods
  async takeScreenshot(name = 'screenshot') {
    const timestamp = Date.now();
    const filename = `${name}-${timestamp}.png`;
    await this.page.screenshot({ 
      path: `test-results/screenshots/${filename}`,
      fullPage: true
    });
    return filename;
  }

  async getElementCount(selector) {
    return await this.page.locator(selector).count();
  }

  // Error handling for flaky operations
  async executeWithRetry(operation, maxRetries = 2, delay = 1000) {
    let lastError;
    
    for (let i = 0; i <= maxRetries; i++) {
      try {
        return await operation();
      } catch (error) {
        lastError = error;
        if (i < maxRetries) {
          console.log(`Operation failed, retrying in ${delay}ms... (${i + 1}/${maxRetries})`);
          await this.page.waitForTimeout(delay);
        }
      }
    }
    
    throw lastError;
  }

  // Dismiss all visible persistent notifications (e.g. INFO alerts with a Dismiss button)

  async dismissAllNotifications() {
    const dismissButtons = this.page
      .locator('[aria-label="Incoming notifications"] button[aria-label="Dismiss"], [aria-label="Incoming notifications"] button:has-text("Dismiss")');
    const count = await dismissButtons.count();
    for (let i = 0; i < count; i++) {
      try {
        await dismissButtons.first().click({ timeout: 3000 });
      } catch {
        break;
      }
    }
    // Wait briefly for the notification overlay to clear
    await this.page.locator('[aria-label="Incoming notifications"]').waitFor({ state: 'hidden', timeout: 3000 }).catch(() => {});
  }

  // Backward compatibility for existing tests
  async clicks(selector, options = {}) {
    await this.click(selector, options);
  }

  async type(selector, text, options = {}) {
    const locator = typeof selector === 'string' ? this.page.locator(selector) : selector;
    await locator.waitFor({ state: 'visible', timeout: this.timeout });
    await locator.pressSequentially(text, options);
  }
}

export { BasePage };