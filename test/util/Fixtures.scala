package util

import models._
import org.joda.time.DateTime

object Fixtures {
  def emptyAmi(arn: String): AMI =
    AMI(arn, None, "", "", None, Map.empty, None, "", "", "", "", "",  "", None, None)
  def emptyInstance(arn: String): Instance =
    Instance(arn, "", "", "", "", "", DateTime.now, "", "", "", Nil, Map.empty, None, None, Nil, Nil, Map.empty, Meta("", Origin("", "", "", "")))
  def instanceWithAmiArn(arn: String, amiArnOpt: Option[String]): Instance =
    amiArnOpt.fold(emptyInstance(arn))(amiArn => emptyInstance(arn).copy(specification = Map("imageArn" -> amiArn)))
  def instanceWithSSA(arn: String, ssa: SSA): Instance =
    emptyInstance(arn).copy(stack = ssa.stack, stage = ssa.stage, app = ssa.app.toList)

  object AMIs {
    val validAMI = """{
                     |  "arn": "arn:aws:ec2:region::image/ami-example",
                     |  "name": "ami-name",
                     |  "imageId": "ami-example",
                     |  "region": "region",
                     |  "description": "AMI description",
                     |  "tags": {
                     |    "Name": "ami-name",
                     |    "SourceAMI": "ami-source",
                     |    "Build": "666",
                     |    "BuildName": "build-name",
                     |    "Branch": "branch-name",
                     |    "VCSRef": "commitref"
                     |  },
                     |  "creationDate": "2016-02-11T00:00:00.000Z",
                     |  "state": "available",
                     |  "architecture": "x86_64",
                     |  "ownerId": "0123456789",
                     |  "virtualizationType": "hvm",
                     |  "hypervisor": "xen",
                     |  "sriovNetSupport": "simple",
                     |  "rootDeviceType": "ebs",
                     |  "imageType": "machine",
                     |  "meta": {
                     |    "href": "http://root/images/arn:aws:ec2:region::image/ami-example",
                     |    "origin": {
                     |      "vendor": "aws",
                     |      "accountName": "account-name",
                     |      "region": "region",
                     |      "accountNumber": "0123456789"
                     |    }
                     |  }
                     |}""".stripMargin

    val validAmiResponse = s"""{"data": $validAMI}"""

    val validAmisResponse = s"""{"data": {"images": [$validAMI]}}"""
  }

  object Instances {
    val validInstance = """{
                          |  "arn": "arn:aws:ec2:region:0123456789:instance/i-id",
                          |  "name": "instance-name",
                          |  "vendorState": "running",
                          |  "group": "region",
                          |  "dnsName": "ip-0.0.0.0.region.compute.internal",
                          |  "ip": "0.0.0.0",
                          |  "addresses": {
                          |    "private": {
                          |      "dnsName": "ip-0.0.0.0.region.compute.internal",
                          |      "ip": "0.0.0.0"
                          |    }
                          |  },
                          |  "createdAt": "2016-02-09T10:59:01.000Z",
                          |  "instanceName": "i-id",
                          |  "region": "region",
                          |  "vendor": "aws",
                          |  "securityGroups": [
                          |    "arn:aws:ec2:region:0123456789:security-group/sg-id"
                          |  ],
                          |  "tags": {
                          |    "aws:autoscaling:groupName": "AutoscalingGroup-id",
                          |    "Name": "name",
                          |    "App": "app",
                          |    "aws:cloudformation:stack-name": "stack-name",
                          |    "Stack": "stack",
                          |    "aws:cloudformation:logical-id": "AutoscalingGroup",
                          |    "aws:cloudformation:stack-id": "arn:aws:cloudformation:region:0123456789:stack/id",
                          |    "Stage": "STAGE"
                          |  },
                          |  "stage": "STAGE",
                          |  "stack": "stack",
                          |  "app": [
                          |    "app"
                          |  ],
                          |  "mainclasses": [
                          |    "stack::app"
                          |  ],
                          |  "management": [
                          |    {
                          |      "protocol": "http",
                          |      "port": 18080,
                          |      "path": "/management",
                          |      "url": "http://ip-0.0.0.0.region.compute.internal:18080/management",
                          |      "format": "gu",
                          |      "source": "convention"
                          |    }
                          |  ],
                          |  "specification": {
                          |    "imageId": "ami-id",
                          |    "imageArn": "arn:aws:ec2:region::image/ami-id",
                          |    "instanceType": "m3.medium",
                          |    "vpcId": "vpc-9f1fb9fa"
                          |  },
                          |  "meta": {
                          |    "href": "http://root/arn:aws:ec2:region:0123456789:instance%2Fi-id",
                          |    "origin": {
                          |      "vendor": "aws",
                          |      "accountName": "aws-account-name",
                          |      "region": "region",
                          |      "accountNumber": "0123456789"
                          |    }
                          |  }
                          |}""".stripMargin
  }

  object LaunchConfigs {
    val validLaunchConfiguration =
      """{
        |        "name": "LaunchConfig-123",
        |        "arn": "arn:aws:autoscaling:us-west-1:12343425:launchConfiguration:123-123-325-d121:launchConfigurationName/LaunchConfig-123",
        |        "imageId": "ami-12345",
        |        "region": "us-west-1",
        |        "createdTime": "2015-01-02T17:00:00.000Z",
        |        "instanceType": "t2.micro",
        |        "keyName": "KeyPair",
        |        "securityGroups": [
        |          "arn:aws:ec2:us-west-1:12343243:security-group/sg-aa111111",
        |          "arn:aws:ec2:us-west-1:12332144:security-group/sg-bb222222",
        |          "arn:aws:ec2:us-west-1:11111111:security-group/sg-cc333333",
        |          "arn:aws:ec2:us-west-1:22222222:security-group/sg-dd444444"
        |        ],
        |        "userData": "UserDataHash=",
        |        "meta": {
        |          "href": "http://localhost:8080/configs/arn:aws:autoscaling:us-west-1:12343425:launchConfiguration:123-123-325-d121:launchConfigurationName:LaunchConfig-1233",
        |          "origin": {
        |            "accountName": "account",
        |            "region": "us-west-1",
        |            "accountNumber": "123456789",
        |            "vendor": "aws",
        |            "credentials": "arn:aws:iam::12345678:role/IamRole-IamRole-1234SASDASD"
        |          }
        |        }
        |      }""".stripMargin
  }
}
