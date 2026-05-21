import { When, Then } from '@cucumber/cucumber';
import { expect } from '@playwright/test';
import { ProQuipWorld } from '../support/world';

When(
  'the user clicks a zone cell in the layout',
  async function (this: ProQuipWorld) {
    await this.page.evaluate(() => {
      const zone = document.querySelector('.zone-cell') as HTMLElement;
      zone?.click();
    });
  }
);

Then(
  'the zone detail should load without server error',
  async function (this: ProQuipWorld) {
    let errorDialog = false;
    this.page.on('dialog', async (dialog) => {
      if (dialog.message().includes('サーバーエラー')) {
        errorDialog = true;
        await dialog.accept();
      }
    });
    await this.page.evaluate(() => {
      const zone = document.querySelector('.zone-cell') as HTMLElement;
      zone?.click();
    });
    await this.page.waitForTimeout(3000);
    expect(errorDialog).toBe(false);
  }
);

Then(
  'the zone cells should be visible',
  async function (this: ProQuipWorld) {
    await this.page.waitForTimeout(3000);
    const zones = await this.page.locator('.zone-cell').count();
    expect(zones).toBeGreaterThan(0);
  }
);
