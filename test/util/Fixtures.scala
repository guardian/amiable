package util

import play.api.libs.json.{JsObject, JsValue}

object Fixtures {
  object JSON {
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
  }
}
