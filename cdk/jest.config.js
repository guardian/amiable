module.exports = {
  testMatch: ["<rootDir>/lib/**/*.test.ts"],
  transform: {
    "^.+\\.tsx?$": "ts-jest",
  },
  testResultsProcessor: "jest-teamcity-reporter",
  transformIgnorePatterns: ["node_modules/(?!@guardian/private-infrastructure-config)"],
  setupFilesAfterEnv: ["./jest.setup.js"],
};
