module.exports = {
  default: {
    requireModule: ['ts-node/register'],
    require: ['step-definitions/**/*.ts', 'support/**/*.ts'],
    paths: ['features/**/*.feature'],
    format: ['progress', 'html:reports/cucumber-report.html'],
    formatOptions: { snippetInterface: 'async-await' },
    publishQuiet: true,
  },
};
