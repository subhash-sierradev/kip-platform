import { expect } from '@playwright/test';

export class ArcGISIntegrationViewPage {
  constructor(page) {
    this.page = page;

    // Breadcrumb / navigation
    this.breadcrumbText = page.getByText('ArcGIS Integration Details');

    // Page heading
    this.pageTitle = page.locator('h1');

    // Tabs
    this.basicDetailsTab = page.getByRole('tab', { name: 'Basic Details' });
    this.scheduleInfoTab = page.getByRole('tab', { name: 'Schedule Info' });
    this.fieldMappingTab = page.getByRole('tab', { name: 'Field Mapping' });
    this.jobHistoryTab = page.getByRole('tab', { name: 'Job History' });

    // Basic Details tab fields (stable CSS class locators from DOM)
    this.integrationNameValue = page.locator('.config-value.name-value');
    this.descriptionValue     = page.locator('.config-value.description-value');
    this.itemSubtypeValue     = page.locator('.config-item')
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

    // Schedule Info tab – day chips (scoped to Days row → tag-list → individual tag spans)
    this.dayChips = page.locator('.info-row')
      .filter({ hasText: /Days/i })
      .locator('.tag-list .tag');

    // Field Mapping tab – all data rows
    this.fieldMappingRows = page.locator('tr.dx-data-row');
  }

  // ── Tab navigation ────────────────────────────────────────────────────────

  //Navigate to and click the Basic Details tab
  async clickBasicDetailsTab() {
    await expect(this.basicDetailsTab).toBeVisible({ timeout: 10000 });
    await this.basicDetailsTab.click();
    await this.page.waitForLoadState('domcontentloaded');
  }

  //Navigate to and click the Schedule Info tab
  async clickScheduleInfoTab() {
    await expect(this.scheduleInfoTab).toBeVisible({ timeout: 10000 });
    await this.scheduleInfoTab.click();
    await this.page.waitForLoadState('domcontentloaded');
  }

  //Navigate to and click the Field Mapping tab
  async clickFieldMappingTab() {
    await expect(this.fieldMappingTab).toBeVisible({ timeout: 10000 });
    await this.fieldMappingTab.click();
    await this.page.waitForLoadState('domcontentloaded');
  }

  //Navigate to and click the Job History tab
  async clickJobHistoryTab() {
    await expect(this.jobHistoryTab).toBeVisible({ timeout: 10000 });
    await this.jobHistoryTab.click();
    await this.page.waitForLoadState('domcontentloaded');
  }

  //Navigate back to the ArcGIS Integration list page using the breadcrumb
  async navigateBackToList() {
    const breadcrumb = this.page.getByRole('navigation', { name: 'Breadcrumb' });
    await breadcrumb.getByText('ArcGIS Integration', { exact: true }).click();
    await this.page.waitForLoadState('domcontentloaded');
  }

  // ── Basic Details ─────────────────────────────────────────────────────────

  //Return the integration name text from the Basic Details tab
  async getIntegrationName() {
    try {
      await expect(this.integrationNameValue).toBeVisible({ timeout: 5000 });
      return (await this.integrationNameValue.textContent())?.trim() || null;
    } catch {
      return null;
    }
  }

  //Return the description text from the Basic Details tab
  async getDescription() {
    try {
      await expect(this.descriptionValue).toBeVisible({ timeout: 5000 });
      return (await this.descriptionValue.textContent())?.trim() || null;
    } catch {
      return null;
    }
  }

  //Return the item subtype text from the Basic Details tab
  async getItemSubtype() {
    try {
      await expect(this.itemSubtypeValue).toBeVisible({ timeout: 5000 });
      return (await this.itemSubtypeValue.textContent())?.trim() || null;
    } catch {
      return null;
    }
  }

  // ── Schedule Info ─────────────────────────────────────────────────────────

  //Return the schedule pattern badge text from the Schedule Info tab
  async getPattern() {
    try {
      await expect(this.patternValue).toBeVisible({ timeout: 5000 });
      return (await this.patternValue.textContent())?.trim() || null;
    } catch {
      return null;
    }
  }

  //Return the schedule pattern detail text (e.g. "Custom schedule pattern") from the Schedule Info tab
  async getPatternDetail() {
    try {
      await expect(this.patternDetailValue).toBeVisible({ timeout: 5000 });
      return (await this.patternDetailValue.textContent())?.trim() || null;
    } catch {
      return null;
    }
  }

  //Return an array of visible day-chip label strings from the Schedule Info tab
  async getScheduleDayChips() {
    const count = await this.dayChips.count();
    const labels = [];
    for (let i = 0; i < count; i++) {
      const text = (await this.dayChips.nth(i).textContent())?.trim();
      if (text) labels.push(text);
    }
    return labels;
  }

  //Return the CRON expression value from the Schedule Info tab
  async getCronExpression() {
    try {
      await expect(this.cronValue).toBeVisible({ timeout: 5000 });
      return (await this.cronValue.textContent())?.trim() || null;
    } catch {
      return null;
    }
  }

  //Return the start date text from the Schedule Info tab
  async getStartDate() {
    try {
      await expect(this.startDateValue).toBeVisible({ timeout: 5000 });
      return (await this.startDateValue.textContent())?.trim() || null;
    } catch {
      return null;
    }
  }

  //Convert a YYYY-MM-DD date string to the display format used by the app
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

  //Return cell text for a specific row in the field mapping grid (Kaseware Field, ArcGIS Field, Transformation)
  async getFieldMappingRow(rowIndex) {
    await this.fieldMappingRows.first().waitFor({ state: 'visible', timeout: 10000 });

    const row = this.fieldMappingRows.nth(rowIndex);

    const kasField   = (await row.locator('[aria-colindex="2"]').first().textContent())?.trim();
    const arcgisField = (await row.locator('[aria-colindex="3"]').first().textContent())?.trim();
    const transform  = (await row.locator('[aria-colindex="4"]').first().textContent())?.trim();

    return { kasField, arcgisField, transform };
  }

  // ── Complete verification ─────────────────────────────────────────────────

  //Run all tab verifications and return a structured result object for basic details, schedule info, and field mapping
  async verifyAllData(config) {
    // ── Basic Details ──
    await this.clickBasicDetailsTab();
    const actualName = await this.getIntegrationName();
    const actualDesc = await this.getDescription();
    const actualSubtype = await this.getItemSubtype();

    // ── Schedule Info ──
    await this.clickScheduleInfoTab();
    const actualPattern = await this.getPattern();
    const actualDays = await this.getScheduleDayChips();
    const actualDate = await this.getStartDate();
    const expectedDate = this.formatStartDate(config?.schedule?.startDate);

    // ── Field Mapping ──
    await this.clickFieldMappingTab();
    const row1 = await this.getFieldMappingRow(0);
    const row2 = await this.getFieldMappingRow(1);

    return {
      basicDetails: {
        nameMatches: actualName === config?.name,
        descMatches: actualDesc === config?.description,
        subtypeMatches: actualSubtype === config?.itemSubtype,
        actualName,
        actualDesc,
        actualSubtype
      },
      scheduleInfo: {
        patternMatches: actualPattern === 'Weekly',
        daysMatchAll: (config?.schedule?.days || []).every(d => actualDays.includes(d)),
        dateMatches: actualDate === expectedDate,
        actualPattern,
        actualDays,
        actualDate,
        expectedDate
      },
      fieldMapping: {
        row1,
        row2
      }
    };
  }
}