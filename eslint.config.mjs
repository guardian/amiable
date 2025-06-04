export default [
  {
    ignores: [
      // ...patterns from .eslintignore...
      "node_modules",
      "dist",
      "coverage",
    ],
  },
  {
    files: ["**/*.ts", "**/*.tsx"],
    languageOptions: {
      parser: "@typescript-eslint/parser",
      parserOptions: {
        ecmaVersion: "latest",
        sourceType: "module",
      },
    },
    plugins: {
      import: require("eslint-plugin-import"),
    },
    extends: [
      // ...existing config from .eslintrc.js...
      "@guardian/eslint-config",
    ],
    rules: {
      // ...existing or custom rules...
    },
  },
];
