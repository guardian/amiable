import { App } from "aws-cdk-lib";
import { Template } from "aws-cdk-lib/assertions";
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

    expect(Template.fromStack(stack).toJSON()).toMatchSnapshot();
  });
});
