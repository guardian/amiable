import { AccessScope } from "@guardian/cdk/lib/constants/access";
import type { GuStackProps } from "@guardian/cdk/lib/constructs/core";
import { GuDistributionBucketParameter, GuStack, GuStringParameter } from "@guardian/cdk/lib/constructs/core";
import { GuCname } from "@guardian/cdk/lib/constructs/dns";
import { GuAllowPolicy, GuSESSenderPolicy } from "@guardian/cdk/lib/constructs/iam";
import type { App } from "aws-cdk-lib";
import { Duration, SecretValue } from "aws-cdk-lib";
import { InstanceClass, InstanceSize, InstanceType, UserData } from "aws-cdk-lib/aws-ec2";
import { ParameterDataType, ParameterTier, StringParameter } from "aws-cdk-lib/aws-ssm";
import {GuLoadBalancedAppExperimental} from "@guardian/cdk/lib/experimental/patterns/gu-load-balanced-app";
import {ListenerAction, UnauthenticatedAction } from "aws-cdk-lib/aws-elasticloadbalancingv2";

interface AmiableProps extends GuStackProps {
  domainName: string;
}

export class Amiable extends GuStack {
  constructor(scope: App, id: string, props: AmiableProps) {
    super(scope, id, props);

    const app = "amiable";
    const { stack, stage } = this;
    const isProd = stage === "PROD";

    const { domainName } = props;

    const distBucket = GuDistributionBucketParameter.getInstance(this).valueAsString;

    const buildNumber = process.env.BUILD_NUMBER ?? "DEV";

    const userData = UserData.forLinux();
    userData.addCommands(`
          mkdir /amiable
          aws --region eu-west-1 s3 cp s3://${distBucket}/${stack}/${stage}/${app}/conf/amiable-service-account-cert.json /amiable/
          aws --region eu-west-1 s3 cp s3://${distBucket}/${stack}/${stage}/${app}/conf/amiable.conf /etc/
          aws --region eu-west-1 s3 cp s3://${distBucket}/${stack}/${stage}/${app}/amiable-${buildNumber}.deb /amiable/amiable.deb

          dpkg -i /amiable/amiable.deb`);

    const loadBalancedApp = new GuLoadBalancedAppExperimental(this, {
      applicationPort: 9000,
      app,
      certificateProps: {
        domainName,
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
      additionalPolicies: [
        new GuSESSenderPolicy(this, { sendingAddress: "dig.dev.tooling@theguardian.com" }),
        new GuAllowPolicy(this, "CloudwatchPolicy", {
          actions: ["cloudwatch:*"],
          resources: ["*"],
        }),
      ],
      ec2Props: {
        versionedDeployments: {
          enabled: true,
          buildIdentifier: buildNumber,
        },
        instanceType: InstanceType.of(InstanceClass.T4G, InstanceSize.SMALL),
        userData,
        instanceMetricGranularity: "5Minute",
        scaling: { minimumInstances: 1 },
        applicationLogging: { enabled: true },
        imageRecipe: "arm64-jammy-java21-deploy-infrastructure",
      }
    });

    // This parameter is used by https://github.com/guardian/waf
    new StringParameter(this, "AlbSsmParam", {
      parameterName: `/infosec/waf/services/${this.stage}/amiable-alb-arn`,
      description: `The arn of the ALB for amiable-${this.stage}. N.B. this parameter is created via cdk`,
      simpleName: false,
      stringValue: loadBalancedApp.loadBalancer.loadBalancerArn,
      tier: ParameterTier.STANDARD,
      dataType: ParameterDataType.TEXT,
    });

    const clientId = new GuStringParameter(this, "ClientId", {
      description: "Google OAuth client ID",
    });

    loadBalancedApp.listener.addAction("DefaultAction", {
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
        next: ListenerAction.forward([loadBalancedApp.targetGroups.ec2!]),
      }),
    });

    new GuCname(this, "DnsRecord", {
      app,
      domainName: domainName,
      ttl: Duration.hours(1),
      resourceRecord: loadBalancedApp.loadBalancer.loadBalancerDnsName,
    });
  }
}
