package util

import models.{AMI, Instance, Meta, Origin}
import org.joda.time.DateTime

object Fixtures {
  def emptyAmi: AMI =
    AMI("", None, "", "", None, Map.empty, None, "", "", "", "", "",  None)
  def emptyInstance(arn: String): Instance =
    Instance(arn, "", "", "", "", "", DateTime.now, "", "", "", Nil, Map.empty, None, None, Nil, Nil, Map.empty, Meta("", Origin("", "", "", "")))
  def instanceWithAmiArn(arn: String, amiArnOpt: Option[String]): Instance =
    amiArnOpt.fold(emptyInstance(arn))(amiArn => emptyInstance(arn).copy(specification = Map("imageArn" -> amiArn)))

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
}
