# Deploy Tools CDK

This directory contains CDK files to define apps that were previously defined
via cloudformation.

The `cdk.json` file tells the CDK Toolkit how to execute your app.

## Useful commands
We follow the [`script/task`](https://github.com/github/scripts-to-rule-them-all) pattern,
find useful scripts within the [`script`](./script) directory for common tasks.

- `./script/setup` to install dependencies
- `./script/start` to run the Jest unit tests in watch mode
- `./script/lint` to lint the code using ESLint
- `./script/test` to lint, run tests and generate templates of the CDK stacks
- `./script/build` to compile TypeScript to JS and generate templates of the CDK stacks
- `./script/diff [--prod]` to show the difference between the CDK template and the stack in AWS
- `./script/generate` to build a CDK stack into the `cdk.out` directory

There are also some other commands defined in `package.json`, including:
- `yarn lint --fix` attempt to autofix any linter errors
- `yarn format` format the code using Prettier
- `yarn watch` watch for changes and compile

However, it's advised you configure your IDE to format on save to avoid horrible "correct linting" commits.

## Changes & Gotchas

### InstanceProfile Path

Previously when we defined instance profiles explicity in Cloudformation, we set the path value to `/`.
Now that the instance profile resource is created automagically by the autoscaling group,
the path value is not set as the default value is `/`.

### LoadBalancer -> AppSecurityGroup Egress

In stacks with a load balancer and autoscaling group we define a security group for each. Previously in
cloudformation templates, we explicitly set an ingress rule to the app security group from the loadbalancer
security group. This ingress is now created automatically as a separate resource by the autoscaling group.
As well as the ingress, an egress from the loadbalancer security group is created.

### AutoscalingGroup Availability Zones

Previously in the cloudformation template, the autoscaling group contains an `AvailabilityZones` property e.g.

```yaml
AvailabilityZones:
  - Fn::Select:
      - 0
      - Fn::GetAZs: ""
  - Fn::Select:
      - 1
      - Fn::GetAZs: ""
```

as well as the `VPCZoneIdentifier` property.

According to the [documentation](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-properties-as-group.html#cfn-as-group-availabilityzones), only one of the properties is required (unless you're using EC2-Classic in which case the AZs property is required.)

The CDK synthesized template does not contain the `AvailabilityZones` property and there is no obvious way of adding it. Accordinly to the
[documentation](https://docs.aws.amazon.com/cdk/api/latest/docs/@aws-cdk_aws-autoscaling.AutoScalingGroup.html) of the component:

```
The ASG spans the availability zones specified by vpcSubnets, falling back to the Vpc default strategy if not specified.
```

which suggests that the `AvailabilityZones` property is not required.

### Security Group Egress

In cloudformation, many security groups did not define an ingress. That is not possible with CDK as you
only have the option to either allow or disallow all outbound traffic. For some stacks, disabling
all outbound traffic has been okay but for others (prism) it has meant that the instances were unable to
pull from s3 and so couldn't launch successfully.

### LoadBalancer listeners for HTTP and HTTPS

In cloudformation, for the LoadBalancer listeners we only set the `Protocol` value which defines the
protocol that a listener rule listens for. In CDK we can define one or both of `internalProtocol` and
`externalProtocol`. If you only define one then it defaults to be the same as the other. This becomes
problematic when forwarding both HTTP and HTTPS traffic to the same port as it causes an error.

If you are doing this you must set the `internalProtocol` to be `HTTP` in both cases.

### RDS DatabaseInstance defaults

By default, an instance of an RDS DatabaseInstance generates a Cloudformation template
with the following default keys and values:

```json
"RDS": {
      "Type": "AWS::RDS::DBInstance",
      "Properties": {
        ...
        "CopyTagsToSnapshot": true
        ...
      }
    }
```

- `CopyTagsToSnapshot` is added and defaults to `true` if not otherwise specified
- `UpdateReplacePolicy` is added and follows the value of `DeletionPolicy` if it is specified

## Other

### Profile

Currently the profile value is set to `does-not-exist` in the `cdk.json` file.
This is a workaround to a known
[issue](https://github.com/aws/aws-cdk/issues/7849) where expired credentials
cause an error when running the `cdk synth` command. As we don't (yet) use any
features which require connecting to an account this does not break anything but
in the future we may actually require valid credentials to generate the
cloudformation.
