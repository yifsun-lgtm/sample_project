import { Then } from '@cucumber/cucumber';
import { expect } from '@playwright/test';
import { ProQuipWorld } from '../support/world';

Then(
  'the source warehouse dropdown should have options',
  async function (this: ProQuipWorld) {
    await this.page.waitForSelector('select', { timeout: 10000 });
    await this.page.waitForTimeout(2000);
    const options = await this.page.locator('select').first().locator('option').count();
    expect(options).toBeGreaterThan(1);
  }
);

Then(
  'the destination warehouse dropdown should have options',
  async function (this: ProQuipWorld) {
    await this.page.waitForSelector('select', { timeout: 10000 });
    await this.page.waitForTimeout(2000);
    const options = await this.page.locator('select').nth(1).locator('option').count();
    expect(options).toBeGreaterThan(1);
  }
);

Then(
  'the page should show the adjustments interface',
  async function (this: ProQuipWorld) {
    const body = await this.page.textContent('body');
    expect(body).not.toContain('在庫IDが無効です');
  }
);

Then(
  'the page should show the stocktaking interface',
  async function (this: ProQuipWorld) {
    const body = await this.page.textContent('body');
    expect(body).not.toContain('在庫IDが無効です');
  }
);
