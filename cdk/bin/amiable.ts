#!/usr/bin/env node
import "source-map-support/register";
import { App } from "@aws-cdk/core";
import type { AmiableProps } from "../lib/amiable/amiable";
import { Amiable } from "../lib/amiable/amiable";

const stackName = process.env.GU_CDK_STACK_NAME;

const app = new App();

const props: Omit<AmiableProps, "stage" | "domainName" | "scaling"> = {
  migratedFromCloudFormation: true,
  stack: "deploy",
  env: { region: "eu-west-1" },
  cloudFormationStackName: stackName,
};

new Amiable(app, "Amiable-CODE", {
  ...props,
  stage: "CODE",
  domainName: "amiable.code.dev-gutools.co.uk",
  scaling: { minimumInstances: 1 },
});

new Amiable(app, "Amiable-PROD", {
  ...props,
  stage: "PROD",
  domainName: "amiable.gutools.co.uk",
  scaling: { minimumInstances: 1 },
});
