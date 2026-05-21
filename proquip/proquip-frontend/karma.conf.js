// Karma設定ファイル
// 技術的負債: カバレッジ閾値が設定されていない
// テストカバレッジの最低ラインを設定すべき（例: statements 80%, branches 70%）
// 参考: https://karma-runner.github.io/

module.exports = function (config) {
  config.set({
    basePath: '',
    frameworks: ['jasmine', '@angular-devkit/build-angular'],
    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('karma-jasmine-html-reporter'),
      require('karma-coverage'),
      require('@angular-devkit/build-angular/plugins/karma')
    ],
    client: {
      jasmine: {
        // Jasmineの設定
        // 技術的負債: ランダム実行順が無効のため、テスト間の依存が検出しにくい
        random: false
      },
      clearContext: false // Jasmine Spec Runnerの出力をブラウザに表示
    },
    jasmineHtmlReporter: {
      suppressAll: true // 重複した失敗メッセージを抑制
    },
    coverageReporter: {
      dir: require('path').join(__dirname, './coverage/proquip-frontend'),
      subdir: '.',
      reporters: [
        { type: 'html' },
        { type: 'text-summary' },
        { type: 'lcovonly' }
      ],
      // 技術的負債: カバレッジ閾値が未設定
      // 以下を有効にしてカバレッジ基準を強制すべき
      // check: {
      //   global: {
      //     statements: 80,
      //     branches: 70,
      //     functions: 80,
      //     lines: 80
      //   }
      // }
    },
    reporters: ['progress', 'kjhtml'],
    port: 9876,
    colors: true,
    logLevel: config.LOG_INFO,
    autoWatch: true,
    browsers: ['ChromeHeadless'],
    // CI環境ではsingleRun: trueに設定
    singleRun: false,
    restartOnFileChange: true
  });
};
