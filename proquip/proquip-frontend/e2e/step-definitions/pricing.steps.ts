import { When } from '@cucumber/cucumber';
import { ProQuipWorld } from '../support/world';

When(
  'the user fills in the price list form',
  async function (this: ProQuipWorld) {
    await this.page.getByRole('textbox', { name: '価格リスト名' }).fill('E2Eテスト価格リスト');
    await this.page.getByRole('textbox', { name: '説明' }).fill('E2Eテスト用');
    await this.page.getByRole('textbox', { name: '有効開始日' }).fill('2026-06-01');
    await this.page.getByRole('textbox', { name: '有効終了日' }).fill('2026-12-31');
  }
);
