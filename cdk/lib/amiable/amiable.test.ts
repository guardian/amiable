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
      env: { region: "eu-west-1" },
      stage: "PROD",
      domainName: "amiable.gutools.co.uk",
      scaling: { minimumInstances: 1 },
    });

    expect(SynthUtils.toCloudFormation(stack)).toMatchSnapshot();
  });
});
