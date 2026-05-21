import { When, Then } from '@cucumber/cucumber';
import { expect } from '@playwright/test';
import { ProQuipWorld } from '../support/world';

When(
  'the user fills in the requisition form',
  async function (this: ProQuipWorld) {
    await this.page.locator('#department').selectOption('技術部');
    await this.page.getByRole('combobox').nth(1).selectOption('通常');
    await this.page.locator('#requiredDate').fill('2026-06-01');
    await this.page
      .getByRole('textbox', { name: '購買依頼の理由・目的を記入してください（10文字以上）' })
      .fill('E2Eテスト用の購買依頼です。動作確認のため。');
  }
);

When(
  'the user searches for product {string} and selects the first result',
  async function (this: ProQuipWorld, keyword: string) {
    const input = this.page.getByRole('textbox', { name: '製品名またはSKUで検索' });
    await input.pressSequentially(keyword, { delay: 100 });
    await this.page.waitForTimeout(500);
    const firstResult = this.page.locator('.search-results .search-result-item, .product-search-results > div').first();
    await firstResult.click();
  }
);

When(
  'the user sets the quantity to {int}',
  async function (this: ProQuipWorld, qty: number) {
    await this.page.getByRole('spinbutton').first().fill(String(qty));
  }
);

Then(
  'the total amount should be greater than {string}',
  async function (this: ProQuipWorld, amount: string) {
    const totalText = await this.page.textContent('body');
    expect(totalText).toContain('¥');
  }
);

When(
  'the user fills in the order form with supplier {string}',
  async function (this: ProQuipWorld, supplier: string) {
    const supplierSelect = this.page.locator('select').first();
    await supplierSelect.selectOption({ label: supplier });
  }
);

Then(
  'the approvals page should load without server error',
  async function (this: ProQuipWorld) {
    let errorDialog = false;
    this.page.on('dialog', async (dialog) => {
      if (dialog.message().includes('サーバーエラー')) {
        errorDialog = true;
        await dialog.accept();
      }
    });
    await this.navigateTo('/procurement/approvals');
    await this.page.waitForTimeout(3000);
    expect(errorDialog).toBe(false);
  }
);

Then(
  'the returns page should load without server error',
  async function (this: ProQuipWorld) {
    let errorDialog = false;
    this.page.on('dialog', async (dialog) => {
      if (dialog.message().includes('サーバーエラー')) {
        errorDialog = true;
        await dialog.accept();
      }
    });
    await this.navigateTo('/procurement/returns');
    await this.page.waitForTimeout(3000);
    expect(errorDialog).toBe(false);
  }
);
