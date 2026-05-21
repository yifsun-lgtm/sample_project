import { JapaneseDatePipe } from './japanese-date.pipe';

/**
 * 日本語日付パイプのテスト
 *
 * 技術的負債: 期待値がハードコードされている
 * テスト環境のタイムゾーンによって結果が変わる可能性がある
 */
describe('JapaneseDatePipe', () => {
  let pipe: JapaneseDatePipe;

  beforeEach(() => {
    pipe = new JapaneseDatePipe();
  });

  it('should transform date', () => {
    // ハードコードされた日付の変換テスト
    const result = pipe.transform('2024-03-15');
    expect(result).toBe('2024年03月15日');
  });

  it('should transform Date object', () => {
    const date = new Date(2026, 4, 15); // 2026年5月15日（月は0始まり）
    const result = pipe.transform(date);
    expect(result).toBe('2026年05月15日');
  });

  it('should include time when specified', () => {
    const date = new Date(2026, 4, 15, 14, 30);
    const result = pipe.transform(date, true);
    expect(result).toBe('2026年05月15日 14:30');
  });

  it('should handle null', () => {
    const result = pipe.transform(null);
    expect(result).toBe('');
  });

  it('should handle undefined', () => {
    const result = pipe.transform(undefined);
    expect(result).toBe('');
  });

  it('should handle invalid date string', () => {
    const result = pipe.transform('invalid-date');
    expect(result).toBe('');
  });
});
