import { AccessScope } from "@guardian/cdk/lib/constants/access";
import type { GuStackProps } from "@guardian/cdk/lib/constructs/core";
import { GuDistributionBucketParameter, GuStack, GuStringParameter } from "@guardian/cdk/lib/constructs/core";
import { GuCname } from "@guardian/cdk/lib/constructs/dns";
import { GuHttpsEgressSecurityGroup } from "@guardian/cdk/lib/constructs/ec2";
import { GuAllowPolicy, GuSESSenderPolicy } from "@guardian/cdk/lib/constructs/iam";
import { GuPlayApp } from "@guardian/cdk/lib/patterns/ec2-app";
import type { App } from "aws-cdk-lib";
import { Duration, SecretValue } from "aws-cdk-lib";
import { InstanceClass, InstanceSize, InstanceType } from "aws-cdk-lib/aws-ec2";
import { ListenerAction, UnauthenticatedAction } from "aws-cdk-lib/aws-elasticloadbalancingv2";

interface AmiableProps extends GuStackProps {
  domainName: string;
}

export class Amiable extends GuStack {
  constructor(scope: App, id: string, props: AmiableProps) {
    super(scope, id, props);

    const app = "amiable";
    const { stack, stage } = this;
    const isProd = stage === "PROD";

    const distBucket = GuDistributionBucketParameter.getInstance(this).valueAsString;

    const ec2App = new GuPlayApp(this, {
      app,
      instanceType: InstanceType.of(InstanceClass.T4G, InstanceSize.SMALL),
      userData: `#!/bin/bash -ev

          mkdir /amiable
          aws --region eu-west-1 s3 cp s3://${distBucket}/${stack}/${stage}/${app}/conf/amiable-service-account-cert.json /amiable/
          aws --region eu-west-1 s3 cp s3://${distBucket}/${stack}/${stage}/${app}/conf/amiable.conf /etc/
          aws --region eu-west-1 s3 cp s3://${distBucket}/${stack}/${stage}/${app}/amiable.deb /amiable/

          dpkg -i /amiable/amiable.deb`,
      certificateProps: {
        domainName: props.domainName,
      },
      monitoringConfiguration: isProd
        ? {
            snsTopicName: "devx-alerts",
            http5xxAlarm: {
              tolerated5xxPercentage: 99,
              numberOfMinutesAboveThresholdBeforeAlarm: 2,
            },
            unhealthyInstancesAlarm: true,
          }
        : { noMonitoring: true },
      access: { scope: AccessScope.PUBLIC },
      roleConfiguration: {
        additionalPolicies: [
          new GuSESSenderPolicy(this),
          new GuAllowPolicy(this, "CloudwatchPolicy", {
            actions: ["cloudwatch:*"],
            resources: ["*"],
          }),
        ],
      },
      applicationLogging: { enabled: true },
      accessLogging: { enabled: true, prefix: `ELBLogs/${stack}/${app}/${stage}` },
      scaling: { minimumInstances: 1 },
    });

    new GuCname(this, "AmiableCname", {
      app,
      domainName: props.domainName,
      ttl: Duration.minutes(1),
      resourceRecord: ec2App.loadBalancer.loadBalancerDnsName,
    });

    // Need to give the ALB outbound access on 443 for the IdP endpoints (to support Google Auth).
    const outboundHttpsSecurityGroup = new GuHttpsEgressSecurityGroup(this, "ldp-access", {
      app: app,
      vpc: ec2App.vpc,
    });

    ec2App.loadBalancer.addSecurityGroup(outboundHttpsSecurityGroup);

    const clientId = new GuStringParameter(this, "ClientId", {
      description: "Google OAuth client ID",
    });

    ec2App.listener.addAction("DefaultAction", {
      action: ListenerAction.authenticateOidc({
        authorizationEndpoint: "https://accounts.google.com/o/oauth2/v2/auth",
        issuer: "https://accounts.google.com",
        scope: "openid",
        authenticationRequestExtraParams: { hd: "guardian.co.uk" },
        onUnauthenticatedRequest: UnauthenticatedAction.AUTHENTICATE,
        tokenEndpoint: "https://oauth2.googleapis.com/token",
        userInfoEndpoint: "https://openidconnect.googleapis.com/v1/userinfo",
        clientId: clientId.valueAsString,
        clientSecret: SecretValue.secretsManager(`/${this.stage}/deploy/amiable/client-secret`),
        next: ListenerAction.forward([ec2App.targetGroup]),
      }),
    });
  }
}
