import "@aws-cdk/assert/jest";
import { SynthUtils } from "@aws-cdk/assert";
import { App } from "@aws-cdk/core";
import { Amiable } from "./amiable";

describe("The Amiable stack", () => {
  it("matches the snapshot", () => {
    const app = new App();
    const stack = new Amiable(app, "amiable", {
      migratedFromCloudFormation: true,
      stack: "deploy",
      stage: "CODE",
      env: { region: "eu-west-1" },
      domainName: "amiable.code.dev-gutools.co.uk",
    });

    expect(SynthUtils.toCloudFormation(stack)).toMatchSnapshot();
  });
});
