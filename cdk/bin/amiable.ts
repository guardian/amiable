#!/usr/bin/env node
import "source-map-support/register";
import { App } from "aws-cdk-lib";
import { Amiable } from "../lib/amiable/amiable";

const app = new App();

const commonProps = { migratedFromCloudFormation: true, stack: "deploy", env: { region: "eu-west-1" } };

export const codeProps = {
  ...commonProps,
  stage: "CODE",
  domainName: "amiable.code.dev-gutools.co.uk",
};

export const prodProps = {
  ...commonProps,
  stage: "PROD",
  domainName: "amiable.gutools.co.uk",
};

new Amiable(app, "Amiable-CODE", codeProps);

new Amiable(app, "Amiable-PROD", prodProps);
