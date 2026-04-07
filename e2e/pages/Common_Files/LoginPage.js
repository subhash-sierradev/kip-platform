import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { BasePage } from './BasePage.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const testData = JSON.parse(fs.readFileSync(path.join(__dirname, '../../utils/userCredentials.json'), 'utf8'));

class LoginPage extends BasePage {

  constructor(page) {
    super(page);
    
    // Page-specific locators (authentication elements are also in this.ui.formElements)
    this.userProfileMenu = page.locator('div').filter({ hasText: /^Org2 Admin$/ }).nth(1);
    this.logoutOption = page.locator('div').filter({ hasText: /^Logout$/ });
  }

  async pageUrlAsync() {
    await this.goto('https://kaseware.sierradev.com');
  }

  async loginAsync() {
    await this.fill(this.ui.formElements.usernameInput, testData.admin.username);
    await this.fill(this.ui.formElements.passwordInput, testData.admin.password);
    await this.click(this.ui.formElements.signInButton);
  }

  async loginAsAppAdminAsync() {
    await this.fill(this.ui.formElements.usernameInput, testData.appAdmin.username);
    await this.fill(this.ui.formElements.passwordInput, testData.appAdmin.password);
    await this.click(this.ui.formElements.signInButton);
    await this.page.waitForLoadState('load');
  }

  async isLoaded() {
    return await this.isVisible(this.ui.headings.signInHeading);
  }

  async logoutAsync() {
    // Click on user profile menu using UICommonElements
    await this.click(this.ui.userProfile.profileMenu);
    // Click on logout option using UICommonElements  
    await this.click(this.ui.userProfile.logoutOption);
    // Wait for redirect to login page
    await this.waitForText("Sign in to your account");
  }
}

export { LoginPage };