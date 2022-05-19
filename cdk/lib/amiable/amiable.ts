import { AccessScope } from "@guardian/cdk/lib/constants/access";
import type { GuStackProps } from "@guardian/cdk/lib/constructs/core";
import { GuStack } from "@guardian/cdk/lib/constructs/core";
import { GuAllowPolicy, GuSESSenderPolicy } from "@guardian/cdk/lib/constructs/iam";
import { GuPlayApp } from "@guardian/cdk/lib/patterns/ec2-app";
import { GuardianPublicNetworks } from "@guardian/private-infrastructure-config";
import type { App } from "aws-cdk-lib";
import { InstanceClass, InstanceSize, InstanceType, Peer } from "aws-cdk-lib/aws-ec2";

interface AmiableProps extends GuStackProps {
  domainName: string;
}

export class Amiable extends GuStack {
  private readonly app: string = "amiable";

  constructor(scope: App, id: string, props: AmiableProps) {
    super(scope, id, props);

    const allowedCIDRs = [
      GuardianPublicNetworks.London,
      GuardianPublicNetworks.NewYork1,
      GuardianPublicNetworks.NewYork2,
    ];

    new GuPlayApp(this, {
      app: this.app,
      instanceType: InstanceType.of(InstanceClass.T4G, InstanceSize.MICRO),
      userData: `#!/bin/bash -ev

          mkdir /amiable
          aws --region eu-west-1 s3 cp s3://deploy-tools-dist/deploy/${this.stage}/amiable/conf/amiable-service-account-cert.json /amiable/
          aws --region eu-west-1 s3 cp s3://deploy-tools-dist/deploy/${this.stage}/amiable/conf/amiable.conf /etc/
          aws --region eu-west-1 s3 cp s3://deploy-tools-dist/deploy/${this.stage}/amiable/amiable.deb /amiable/

          dpkg -i /amiable/amiable.deb`,
      certificateProps: {
        domainName: props.domainName,
      },
      monitoringConfiguration:
        this.stage === "PROD"
          ? {
              snsTopicName: "devx-alerts",
              http5xxAlarm: {
                tolerated5xxPercentage: 99,
                numberOfMinutesAboveThresholdBeforeAlarm: 2,
              },
              unhealthyInstancesAlarm: true,
            }
          : { noMonitoring: true },
      access: { scope: AccessScope.RESTRICTED, cidrRanges: allowedCIDRs.map((cidr) => Peer.ipv4(cidr)) },
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
      accessLogging: { enabled: true, prefix: `ELBLogs/${this.stack}/${this.app}/${this.stage}` },
      scaling: { minimumInstances: 1 },
    });
  }
}
