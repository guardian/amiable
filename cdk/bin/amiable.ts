#!/usr/bin/env node
import "source-map-support/register";
import { App } from "@aws-cdk/core";
import { Amiable } from "../lib/amiable/amiable";

const app = new App();
new Amiable(app, "Amiable", {
  migratedFromCloudFormation: true,
  stack: "deploy",
  env: { region: "eu-west-1" },
});
