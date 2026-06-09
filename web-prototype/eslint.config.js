import js from '@eslint/js';
import globals from 'globals';

export default [
  {
    ignores: ['dist/**/*', 'node_modules/**/*', '.venv/**/*', 'coverage/**/*']
  },
  js.configs.recommended,
  {
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: 'module',
      globals: {
        ...globals.browser,
        ...globals.node,
        Chart: 'readonly'
      }
    },
    rules: {
      'no-unused-vars': 'warn',
      'no-console': ['warn', { allow: ['warn', 'error', 'log'] }],
      'semi': ['error', 'always'],
      'quotes': ['error', 'single', { 'avoidEscape': true }]
    }
  }
];
