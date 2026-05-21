import { Given, When, Then } from '@cucumber/cucumber';
import { expect } from '@playwright/test';
import { ProQuipWorld } from '../support/world';

// ---------------------------------------------------------------------------
// Navigation
// ---------------------------------------------------------------------------

Given(
  'the user navigates to {string}',
  async function (this: ProQuipWorld, path: string) {
    await this.navigateTo(path);
  }
);

// ---------------------------------------------------------------------------
// Page assertions
// ---------------------------------------------------------------------------

Then(
  'the page title should be {string}',
  async function (this: ProQuipWorld, title: string) {
    await expect(this.page).toHaveTitle(title);
  }
);

Then(
  'the page should contain {string}',
  async function (this: ProQuipWorld, text: string) {
    const body = await this.page.textContent('body');
    expect(body).toContain(text);
  }
);

Then(
  'the page should not contain {string}',
  async function (this: ProQuipWorld, text: string) {
    const body = await this.page.textContent('body');
    expect(body).not.toContain(text);
  }
);

Then(
  'the heading {string} should be visible',
  async function (this: ProQuipWorld, heading: string) {
    const h = this.page.getByRole('heading', { name: heading });
    await expect(h).toBeVisible();
  }
);

Then(
  'the URL should contain {string}',
  async function (this: ProQuipWorld, path: string) {
    expect(this.page.url()).toContain(path);
  }
);

// ---------------------------------------------------------------------------
// Click / Input
// ---------------------------------------------------------------------------

When(
  'the user clicks the {string} button',
  async function (this: ProQuipWorld, name: string) {
    await this.page.getByRole('button', { name }).click();
  }
);

When(
  'the user clicks the {string} link',
  async function (this: ProQuipWorld, name: string) {
    await this.page.getByRole('link', { name }).click();
  }
);

When(
  'the user clicks the first row in the table',
  async function (this: ProQuipWorld) {
    await this.page.locator('table tbody tr').first().click();
    await this.page.waitForLoadState('networkidle');
    await this.page.waitForTimeout(500);
  }
);

When(
  'the user fills in {string} with {string}',
  async function (this: ProQuipWorld, label: string, value: string) {
    await this.page.getByRole('textbox', { name: label }).fill(value);
  }
);

When(
  'the user selects {string} from {string}',
  async function (this: ProQuipWorld, option: string, label: string) {
    await this.page.getByRole('combobox', { name: label }).selectOption({ label: option });
  }
);

When(
  'the user waits {int} milliseconds',
  async function (this: ProQuipWorld, ms: number) {
    await this.page.waitForTimeout(ms);
  }
);

// ---------------------------------------------------------------------------
// Table assertions
// ---------------------------------------------------------------------------

Then(
  'the table should have more than {int} rows',
  async function (this: ProQuipWorld, count: number) {
    const rows = await this.page.locator('table tbody tr').count();
    expect(rows).toBeGreaterThan(count);
  }
);

Then(
  'the table should have {int} rows',
  async function (this: ProQuipWorld, count: number) {
    const rows = await this.page.locator('table tbody tr').count();
    expect(rows).toBe(count);
  }
);

Then(
  'the table column {int} should not contain {string}',
  async function (this: ProQuipWorld, col: number, text: string) {
    const cells = await this.getTableCellTexts('table tbody tr', col);
    for (const cell of cells) {
      expect(cell).not.toContain(text);
    }
  }
);

Then(
  'the table column {int} should not have empty cells',
  async function (this: ProQuipWorld, col: number) {
    const cells = await this.getTableCellTexts('table tbody tr', col);
    for (const cell of cells) {
      expect(cell.trim().length).toBeGreaterThan(0);
    }
  }
);

Then(
  'the table column {int} values should all be translated',
  async function (this: ProQuipWorld, col: number) {
    const untranslated = [
      'SUBMITTED', 'PARTIALLY_ORDERED', 'INVOICED', 'RECEIVED',
      'PARTIALLY_RECEIVED', 'CLOSED', 'PENDING',
    ];
    const cells = await this.getTableCellTexts('table tbody tr', col);
    for (const cell of cells) {
      for (const raw of untranslated) {
        expect(cell).not.toBe(raw);
      }
    }
  }
);

// ---------------------------------------------------------------------------
// Pagination
// ---------------------------------------------------------------------------

Then(
  'the pagination should show {string}',
  async function (this: ProQuipWorld, text: string) {
    const body = await this.page.textContent('body');
    expect(body).toContain(text);
  }
);

// ---------------------------------------------------------------------------
// Select / Dropdown
// ---------------------------------------------------------------------------

Then(
  'the select {string} should have more than {int} options',
  async function (this: ProQuipWorld, label: string, count: number) {
    const options = await this.page
      .getByRole('combobox', { name: label })
      .locator('option')
      .count();
    expect(options).toBeGreaterThan(count);
  }
);

Then(
  'the combobox {string} should have more than {int} options',
  async function (this: ProQuipWorld, selector: string, count: number) {
    const options = await this.page.locator(`${selector} option`).count();
    expect(options).toBeGreaterThan(count);
  }
);

// ---------------------------------------------------------------------------
// Tab panel assertions
// ---------------------------------------------------------------------------

Then(
  'the active tab panel should have content',
  async function (this: ProQuipWorld) {
    const panel = this.page.locator('.tab-panel');
    await expect(panel).toBeVisible();
    const text = await panel.textContent();
    expect(text!.trim().length).toBeGreaterThan(0);
  }
);

// ---------------------------------------------------------------------------
// KPI card assertions
// ---------------------------------------------------------------------------

Then(
  'the KPI card {string} should display a number',
  async function (this: ProQuipWorld, cardTitle: string) {
    const card = this.page.locator('.summary-card', { hasText: cardTitle });
    const value = await card.locator('.card-main-value').textContent();
    expect(value).toBeTruthy();
    expect(value!.trim()).toMatch(/\d/);
  }
);

// ---------------------------------------------------------------------------
// Element count assertions
// ---------------------------------------------------------------------------

Then(
  'the page should have more than {int} {string} elements',
  async function (this: ProQuipWorld, count: number, selector: string) {
    const elements = await this.page.locator(selector).count();
    expect(elements).toBeGreaterThan(count);
  }
);

// ---------------------------------------------------------------------------
// Console error assertions
// ---------------------------------------------------------------------------

Then(
  'the console should have no errors',
  async function (this: ProQuipWorld) {
    const relevant = this.consoleErrors.filter(
      (e) => !e.includes('401') && !e.includes('favicon')
    );
    expect(relevant).toHaveLength(0);
  }
);

Then(
  'the console should have no errors matching {string}',
  async function (this: ProQuipWorld, pattern: string) {
    const matching = this.consoleErrors.filter((e) => e.includes(pattern));
    expect(matching).toHaveLength(0);
  }
);

Then(
  'the console should have errors matching {string}',
  async function (this: ProQuipWorld, pattern: string) {
    const matching = this.consoleErrors.filter((e) => e.includes(pattern));
    expect(matching.length).toBeGreaterThan(0);
  }
);

// ---------------------------------------------------------------------------
// Dialog assertions
// ---------------------------------------------------------------------------

Then(
  'a server error dialog should appear',
  async function (this: ProQuipWorld) {
    const dialogPromise = this.page.waitForEvent('dialog', { timeout: 5000 });
    const dialog = await dialogPromise;
    expect(dialog.message()).toContain('サーバーエラー');
    await dialog.accept();
  }
);

Then(
  'no server error dialog should appear',
  async function (this: ProQuipWorld) {
    let dialogAppeared = false;
    const handler = () => { dialogAppeared = true; };
    this.page.on('dialog', handler);
    await this.page.waitForTimeout(2000);
    this.page.off('dialog', handler);
    expect(dialogAppeared).toBe(false);
  }
);

// ---------------------------------------------------------------------------
// Form save + server error
// ---------------------------------------------------------------------------

When(
  'the user clicks {string} and a server error dialog appears',
  async function (this: ProQuipWorld, buttonName: string) {
    const dialogPromise = this.page.waitForEvent('dialog', { timeout: 10000 });
    await this.page.getByRole('button', { name: buttonName }).click();
    const dialog = await dialogPromise;
    expect(dialog.message()).toContain('サーバーエラー');
    await dialog.accept();
  }
);
