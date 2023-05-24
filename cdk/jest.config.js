module.exports = {
  testMatch: ["<rootDir>/lib/**/*.test.ts"],
  transform: {
    "^.+\\.tsx?$": "ts-jest",
  },
  transformIgnorePatterns: ["node_modules/(?!@guardian/private-infrastructure-config)"],
  setupFilesAfterEnv: ["./jest.setup.js"],
};
