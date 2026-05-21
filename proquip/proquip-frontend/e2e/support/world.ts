import { World, setWorldConstructor, IWorldOptions } from '@cucumber/cucumber';
import { Browser, BrowserContext, Page, chromium } from '@playwright/test';

export class ProQuipWorld extends World {
  browser!: Browser;
  context!: BrowserContext;
  page!: Page;
  baseUrl = process.env['BASE_URL'] || 'http://localhost:4200';
  consoleErrors: string[] = [];

  constructor(options: IWorldOptions) {
    super(options);
  }

  async init(): Promise<void> {
    this.browser = await chromium.launch({ headless: true });
    this.context = await this.browser.newContext({
      viewport: { width: 1280, height: 720 },
      locale: 'ja-JP',
    });
    this.page = await this.context.newPage();
    this.consoleErrors = [];
    this.page.on('console', (msg) => {
      if (msg.type() === 'error') {
        this.consoleErrors.push(msg.text());
      }
    });
    await this.login();
  }

  async login(): Promise<void> {
    await this.page.goto(`${this.baseUrl}/dashboard`, { waitUntil: 'networkidle' });
    if (this.page.url().includes('/auth/') || this.page.url().includes('/realms/')) {
      await this.page.fill('#username', 'admin');
      await this.page.fill('#password', 'admin123');
      await this.page.click('#kc-login');
      await this.page.waitForURL(`${this.baseUrl}/**`, { timeout: 10000 });
    }
  }

  async cleanup(): Promise<void> {
    await this.page?.close();
    await this.context?.close();
    await this.browser?.close();
  }

  async navigateTo(path: string): Promise<void> {
    await this.page.goto(`${this.baseUrl}${path}`, { waitUntil: 'networkidle' });
  }

  async waitForSelector(selector: string, timeout = 5000): Promise<void> {
    await this.page.waitForSelector(selector, { timeout });
  }

  async getTextContent(selector: string): Promise<string> {
    const el = await this.page.$(selector);
    return el ? (await el.textContent()) || '' : '';
  }

  async getSelectOptions(selector: string): Promise<string[]> {
    return this.page.$$eval(`${selector} option`, (opts) =>
      opts.map((o) => o.textContent?.trim() || '')
    );
  }

  async getTableCellTexts(rowSelector: string, colIndex: number): Promise<string[]> {
    return this.page.$$eval(
      `${rowSelector} td:nth-child(${colIndex})`,
      (cells) => cells.map((c) => c.textContent?.trim() || '')
    );
  }

  async clickAndWaitForNavigation(selector: string): Promise<void> {
    await Promise.all([
      this.page.waitForNavigation({ waitUntil: 'networkidle' }),
      this.page.click(selector),
    ]);
  }
}

setWorldConstructor(ProQuipWorld);
