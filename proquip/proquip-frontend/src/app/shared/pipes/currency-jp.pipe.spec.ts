import { CurrencyJpPipe } from './currency-jp.pipe';

/**
 * 日本円フォーマットパイプのテスト
 *
 * 技術的負債: toLocaleStringの出力がテスト環境（ロケール設定）に依存する可能性
 */
describe('CurrencyJpPipe', () => {
  let pipe: CurrencyJpPipe;

  beforeEach(() => {
    pipe = new CurrencyJpPipe();
  });

  it('should format currency', () => {
    const result = pipe.transform(1234567);
    expect(result).toBe('¥1,234,567');
  });

  it('should format small amount', () => {
    const result = pipe.transform(100);
    expect(result).toBe('¥100');
  });

  it('should format without symbol', () => {
    const result = pipe.transform(5000, false);
    expect(result).toBe('5,000');
  });

  it('should handle zero', () => {
    const result = pipe.transform(0);
    expect(result).toBe('¥0');
  });

  it('should handle null', () => {
    const result = pipe.transform(null);
    expect(result).toBe('');
  });

  it('should handle undefined', () => {
    const result = pipe.transform(undefined);
    expect(result).toBe('');
  });
});
