import expoConfig from 'eslint-config-expo/flat.js';
import globals from 'globals';

const IMPORT_ORDER = {
  groups: ['builtin', 'external', 'internal', 'parent', 'sibling', 'index'],
  'newlines-between': 'always',
  alphabetize: { order: 'asc', caseInsensitive: true },
};

export default [
  {
    ignores: [
      'node_modules/**',
      'ios/**',
      'android/**',
      '.expo/**',
      'dist/**',
      'coverage/**',
      'report/**',
    ],
  },
  ...expoConfig,
  {
    files: ['app/**/*.{js,ts}', 'src/**/*.{js,ts}'],
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
    },
    settings: {
      'import/resolver': { node: { extensions: ['.js', '.ts', '.json'] } },
    },
    rules: {
      eqeqeq: ['error', 'always', { null: 'ignore' }],
      'no-console': 'error',
      'no-var': 'error',
      'prefer-const': 'error',
      'no-unused-vars': ['error', { argsIgnorePattern: '^_', ignoreRestSiblings: true }],
      'import/no-cycle': 'error',
      'import/order': ['error', IMPORT_ORDER],
    },
  },
  {
    files: ['src/**/*.{js,ts}'],
    rules: { 'import/no-default-export': 'error' },
  },
  {
    files: ['app/**/*.ts', 'src/**/*.ts'],
    rules: {
      'no-unused-vars': 'off',
      '@typescript-eslint/no-unused-vars': ['error', {
        argsIgnorePattern: '^_',
        ignoreRestSiblings: true,
      }],
    },
  },
  {
    files: ['scripts/**/*.js', 'eslint.config.js'],
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      globals: globals.node,
    },
    rules: {
      eqeqeq: ['error', 'always', { null: 'ignore' }],
      'no-console': 'error',
      'no-var': 'error',
      'prefer-const': 'error',
      'no-unused-vars': ['error', { argsIgnorePattern: '^_', ignoreRestSiblings: true }],
      'import/no-cycle': 'error',
      'import/order': ['error', IMPORT_ORDER],
    },
  },
];
