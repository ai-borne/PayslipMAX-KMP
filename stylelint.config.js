export default {
  extends: ['stylelint-config-standard'],
  ignoreFiles: ['dist/**/*', 'node_modules/**/*', '.venv/**/*'],
  rules: {
    'color-function-notation': 'modern',
    'alpha-value-notation': 'number',
    'import-notation': 'string'
  }
};
