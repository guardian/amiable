import { AccessScope } from "@guardian/cdk/lib/constants/access";
import type { GuStackProps } from "@guardian/cdk/lib/constructs/core";
import { GuDistributionBucketParameter, GuStack, GuStringParameter } from "@guardian/cdk/lib/constructs/core";
import { GuCname } from "@guardian/cdk/lib/constructs/dns";
import { GuHttpsEgressSecurityGroup } from "@guardian/cdk/lib/constructs/ec2";
import { GuAllowPolicy, GuSESSenderPolicy } from "@guardian/cdk/lib/constructs/iam";
import { GuEc2AppExperimental } from "@guardian/cdk/lib/experimental/patterns/ec2-app";
import type { App } from "aws-cdk-lib";
import { Duration, SecretValue } from "aws-cdk-lib";
import { InstanceClass, InstanceSize, InstanceType, UserData } from "aws-cdk-lib/aws-ec2";
import { ListenerAction, UnauthenticatedAction } from "aws-cdk-lib/aws-elasticloadbalancingv2";
import { ParameterDataType, ParameterTier, StringParameter } from "aws-cdk-lib/aws-ssm";

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

    const ec2App = new GuEc2AppExperimental(this, {
      applicationPort: 9000,
      buildIdentifier: buildNumber,
      app,
      instanceType: InstanceType.of(InstanceClass.T4G, InstanceSize.SMALL),
      userData,
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
      roleConfiguration: {
        additionalPolicies: [
          new GuSESSenderPolicy(this, { sendingAddress: "dig.dev.tooling@theguardian.com" }),
          new GuAllowPolicy(this, "CloudwatchPolicy", {
            actions: ["cloudwatch:*"],
            resources: ["*"],
          }),
        ],
      },
      applicationLogging: { enabled: true },
      scaling: { minimumInstances: 1 },
      imageRecipe: "arm64-noble-java21-deploy-infrastructure",
      instanceMetricGranularity: "5Minute"
    });

    // Need to give the ALB outbound access on 443 for the IdP endpoints (to support Google Auth).
    const outboundHttpsSecurityGroup = new GuHttpsEgressSecurityGroup(this, "ldp-access", {
      app: app,
      vpc: ec2App.vpc,
    });

    ec2App.loadBalancer.addSecurityGroup(outboundHttpsSecurityGroup);

    // This parameter is used by https://github.com/guardian/waf
    new StringParameter(this, "AlbSsmParam", {
      parameterName: `/infosec/waf/services/${this.stage}/amiable-alb-arn`,
      description: `The arn of the ALB for amiable-${this.stage}. N.B. this parameter is created via cdk`,
      simpleName: false,
      stringValue: ec2App.loadBalancer.loadBalancerArn,
      tier: ParameterTier.STANDARD,
      dataType: ParameterDataType.TEXT,
    });

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

    new GuCname(this, "DnsRecord", {
      app,
      domainName: domainName,
      ttl: Duration.hours(1),
      resourceRecord: ec2App.loadBalancer.loadBalancerDnsName,
    });
  }
}
