import globals from 'globals';
import js from "@eslint/js";
import compat from "eslint-plugin-compat";

export default [
  js.configs.recommended,
  compat.configs["flat/recommended"],
  {
    languageOptions: {
      globals: {
        ...globals.browser,
        ...globals.jquery,
        PF: 'readonly',
        PrimeFaces: 'readonly',
      },
    },

    rules: {
      'no-unused-vars': 'off',

      'no-use-before-define': ['error', {
        functions: false,
        classes: false,
        variables: true,
      }],

      'no-alert': 'warn',
      'max-len': ['warn', {
        code: 150,
      }],
    },
  },
];
