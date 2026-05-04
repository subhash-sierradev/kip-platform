import { expect } from '@playwright/test';
import { BasePage } from '../Common_Files/BasePage.js';

export class ArcGISIntegrationViewPage extends BasePage {
  constructor(page) {
    super(page);

    // Breadcrumb / navigation
    this.breadcrumbText = page.getByText('ArcGIS Integration Details');

    // Page heading
    this.pageTitle = page.locator('h1');

    // Tabs
    this.basicDetailsTab = page.getByRole('tab', { name: 'Basic Details' });
    this.scheduleInfoTab = page.getByRole('tab', { name: 'Schedule Info' });
    this.fieldMappingTab = page.getByRole('tab', { name: 'Field Mapping' });
    this.jobHistoryTab = page.getByRole('tab', { name: 'Job History' });

    // Basic Details tab fields
    this.integrationNameValue = page.locator('.config-value.name-value');
    this.descriptionValue = page.locator('.config-value.description-value');
    this.itemSubtypeValue = page.locator('.config-item')
      .filter({ hasText: /Item Subtype/i })
      .locator('.config-value');

    // Schedule Info tab fields
    this.patternValue = page.locator('.info-row')
      .filter({ hasText: /Pattern/i })
      .locator('[class*="frequency-badge"]');
    this.patternDetailValue = page.locator('.info-grid span.frequency-detail');
    this.cronValue = page.locator('code.cron-minimal');
    this.startDateValue = page.locator('.info-row')
      .filter({ hasText: /Start Date/i })
      .locator('.value');

    // Day chips (scoped to Days row)
    this.dayChips = page.locator('.info-row')
      .filter({ hasText: /Days/i })
      .locator('.tag-list .tag');

    // Field Mapping tab – all data rows
    this.fieldMappingRows = page.locator('tr.dx-data-row');
  }

  // ── Tab navigation ────────────────────────────────────────────────────────

  async clickBasicDetailsTab() {
    await expect(this.basicDetailsTab).toBeVisible({ timeout: 10000 });
    await this.basicDetailsTab.click();
  }

  async clickScheduleInfoTab() {
    await expect(this.scheduleInfoTab).toBeVisible({ timeout: 10000 });
    await this.scheduleInfoTab.click();
  }

  async clickFieldMappingTab() {
    await expect(this.fieldMappingTab).toBeVisible({ timeout: 10000 });
    await this.fieldMappingTab.click();
  }

  async clickJobHistoryTab() {
    await expect(this.jobHistoryTab).toBeVisible({ timeout: 10000 });
    await this.jobHistoryTab.click();
  }

  async navigateBackToList() {
    const breadcrumb = this.page.getByRole('navigation', { name: 'Breadcrumb' });
    await breadcrumb.getByText('ArcGIS Integration', { exact: true }).click();
    await this.page.waitForLoadState('domcontentloaded');
  }

  // ── Basic Details ─────────────────────────────────────────────────────────

  async getIntegrationName() {
    try {
      await expect(this.integrationNameValue).toBeVisible({ timeout: 5000 });
      return (await this.integrationNameValue.textContent())?.trim() || null;
    } catch {
      return null;
    }
  }

  async getDescription() {
    try {
      await expect(this.descriptionValue).toBeVisible({ timeout: 5000 });
      return (await this.descriptionValue.textContent())?.trim() || null;
    } catch {
      return null;
    }
  }

  async getItemSubtype() {
    try {
      await expect(this.itemSubtypeValue).toBeVisible({ timeout: 5000 });
      return (await this.itemSubtypeValue.textContent())?.trim() || null;
    } catch {
      return null;
    }
  }

  // ── Schedule Info ─────────────────────────────────────────────────────────

  async getPattern() {
    try {
      await expect(this.patternValue).toBeVisible({ timeout: 5000 });
      return (await this.patternValue.textContent())?.trim() || null;
    } catch {
      return null;
    }
  }

  async getPatternDetail() {
    try {
      await expect(this.patternDetailValue).toBeVisible({ timeout: 5000 });
      return (await this.patternDetailValue.textContent())?.trim() || null;
    } catch {
      return null;
    }
  }

  async getScheduleDayChips() {
    const count = await this.dayChips.count();
    const labels = [];
    for (let i = 0; i < count; i++) {
      const text = (await this.dayChips.nth(i).textContent())?.trim();
      if (text) labels.push(text);
    }
    return labels;
  }

  async getCronExpression() {
    try {
      await expect(this.cronValue).toBeVisible({ timeout: 5000 });
      return (await this.cronValue.textContent())?.trim() || null;
    } catch {
      return null;
    }
  }

  async getStartDate() {
    try {
      await expect(this.startDateValue).toBeVisible({ timeout: 5000 });
      return (await this.startDateValue.textContent())?.trim() || null;
    } catch {
      return null;
    }
  }

  formatStartDate(isoDate) {
    if (!isoDate) return null;
    const [year, month, day] = isoDate.split('-').map(Number);
    const date = new Date(year, month - 1, day);
    return date.toLocaleDateString('en-US', {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
      year: 'numeric'
    });
  }

  // ── Field Mapping grid ────────────────────────────────────────────────────

  async getFieldMappingRow(rowIndex) {
    await this.fieldMappingRows.first().waitFor({ state: 'visible', timeout: 10000 });
    const row = this.fieldMappingRows.nth(rowIndex);
    const kasField = (await row.locator('[aria-colindex="2"]').first().textContent())?.trim();
    const arcgisField = (await row.locator('[aria-colindex="3"]').first().textContent())?.trim();
    const transform = (await row.locator('[aria-colindex="4"]').first().textContent())?.trim();
    return { kasField, arcgisField, transform };
  }
}