import { Peer } from "@aws-cdk/aws-ec2";
import type { App } from "@aws-cdk/core";
import { GuStack } from "@guardian/cdk/lib/constructs/core";
import type { GuStackProps } from "@guardian/cdk/lib/constructs/core";
import { GuAllowPolicy, GuSESSenderPolicy } from "@guardian/cdk/lib/constructs/iam";
import { AccessScope, GuPlayApp } from "@guardian/cdk/lib/patterns/ec2-app";
import { GuardianPublicNetworks } from "@guardian/private-infrastructure-config";

export class Amiable extends GuStack {
  private readonly app: string = "amiable";

  constructor(scope: App, id: string, props: GuStackProps) {
    super(scope, id, props);

    const allowedCIDRs = [
      GuardianPublicNetworks.London,
      GuardianPublicNetworks.NewYork1,
      GuardianPublicNetworks.NewYork2,
    ];

    new GuPlayApp(this, {
      app: this.app,
      userData: `#!/bin/bash -ev

          mkdir /amiable
          aws --region eu-west-1 s3 cp s3://deploy-tools-dist/deploy/${this.stage}/amiable/conf/amiable-service-account-cert.json /amiable/
          aws --region eu-west-1 s3 cp s3://deploy-tools-dist/deploy/${this.stage}/amiable/conf/amiable.conf /etc/
          aws --region eu-west-1 s3 cp s3://deploy-tools-dist/deploy/${this.stage}/amiable/amiable.deb /amiable/

          dpkg -i /amiable/amiable.deb`,
      certificateProps: {
        CODE: { domainName: "amiable.code.dev-gutools.co.uk" },
        PROD: { domainName: "amiable.gutools.co.uk" },
      },
      monitoringConfiguration: { noMonitoring: true },
      access: { scope: AccessScope.RESTRICTED, cidrRanges: allowedCIDRs.map((cidr) => Peer.ipv4(cidr)) },
      roleConfiguration: {
        withoutLogShipping: true,
        additionalPolicies: [
          new GuSESSenderPolicy(this),
          new GuAllowPolicy(this, "CloudwatchPolicy", {
            actions: ["cloudwatch:*"],
            resources: ["*"],
          }),
        ],
      },
      accessLogging: { enabled: true, prefix: `ELBLogs/${this.stack}/${this.app}/${this.stage}` },
      scaling: {
        CODE: { minimumInstances: 1 },
        PROD: { minimumInstances: 1 },
      },
    });
  }
}
