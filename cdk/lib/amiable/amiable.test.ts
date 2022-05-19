import { App } from "aws-cdk-lib";
import { Template } from "aws-cdk-lib/assertions";
import { codeProps, prodProps } from "../../bin/amiable";
import { Amiable } from "./amiable";

describe("The Amiable stack", () => {
  it("matches the snapshot", () => {
    const app = new App();

    const codeStack = new Amiable(app, "amiable", codeProps);
    const prodStack = new Amiable(app, "Amiable-PROD", prodProps);

    expect(Template.fromStack(codeStack).toJSON()).toMatchSnapshot();
    expect(Template.fromStack(prodStack).toJSON()).toMatchSnapshot();
  });
});
