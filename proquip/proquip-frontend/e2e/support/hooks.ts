import { Before, After, setDefaultTimeout } from '@cucumber/cucumber';
import { ProQuipWorld } from './world';

setDefaultTimeout(30_000);

Before(async function (this: ProQuipWorld) {
  await this.init();
});

After(async function (this: ProQuipWorld) {
  await this.cleanup();
});
