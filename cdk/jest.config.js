module.exports = {
  testMatch: ["<rootDir>/lib/**/*.test.ts"],
  transform: {
    "^.+\\.tsx?$": "ts-jest",
  },
  testResultsProcessor: "jest-teamcity-reporter",
  transformIgnorePatterns: ["node_modules/(?!@guardian/private-infrastructure-config)"],
  setupFilesAfterEnv: ["./jest.setup.js"],

  // Preserve snapshot format during initial Jest 29 upgrade.
  // See https://jestjs.io/docs/upgrading-to-jest29.
  // TODO remove this!
  snapshotFormat: {
    escapeString: true,
    printBasicPrototype: true,
  },
};
