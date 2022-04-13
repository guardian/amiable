#!/usr/bin/env node
import "source-map-support/register";
import { App } from "@aws-cdk/core";
import { Amiable } from "../lib/amiable/amiable";

const app = new App();
new Amiable(app, "Amiable-CODE", {
  migratedFromCloudFormation: true,
  stack: "deploy",
  stage: "CODE",
  env: { region: "eu-west-1" },
  domainName: "amiable.code.dev-gutools.co.uk",
});

new Amiable(app, "Amiable-PROD", {
  migratedFromCloudFormation: true,
  stack: "deploy",
  stage: "PROD",
  env: { region: "eu-west-1" },
  domainName: "amiable.gutools.co.uk",
});
