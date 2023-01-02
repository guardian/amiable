package prism

import org.joda.time.DateTime
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.EitherValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.OptionValues
import play.api.libs.ws.WSResponse
import util.AttemptValues
import util.Fixtures._
import play.api.libs.json._
import prism.JsonUtils._

import scala.concurrent.ExecutionContext.Implicits.global

class JsonUtilsTest
    extends AnyFreeSpec
    with Matchers
    with EitherValues
    with AttemptValues
    with OptionValues
    with MockitoSugar {
  "extractAMI" - {
    "should return an AMI from correct json" in {
      val json = Json.parse(AMIs.validAMI)
      val ami = extractAMI(json).awaitEither.right.value
      ami should have(
        'arn("arn:aws:ec2:region::image/ami-example"),
        'name(Some("ami-name")),
        'imageId("ami-example"),
        'region("region"),
        'description(Some("AMI description")),
        'creationDate(Some(new DateTime(2016, 2, 11, 0, 0))),
        'state("available"),
        'architecture("x86_64"),
        'ownerId("0123456789"),
        'virtualizationType("hvm"),
        'hypervisor("xen"),
        'rootDeviceType("ebs"),
        'sriovNetSupport(Some("simple")),
        'upgrade(None)
      )
    }

    "should extract tags from valid AMI JSON" in {
      val json = Json.parse(AMIs.validAMI)
      val ami = extractAMI(json).awaitEither.right.value
      ami.tags shouldEqual Map(
        "Name" -> "ami-name",
        "SourceAMI" -> "ami-source",
        "Build" -> "666",
        "BuildName" -> "build-name",
        "Branch" -> "branch-name",
        "VCSRef" -> "commitref"
      )
    }

    "should return a failure given invalid json" in {
      val jsonStr = """{"testyinvalid":123}"""
      val json = Json.parse(jsonStr)
      extractAMI(json).awaitEither.isLeft shouldBe true
    }
  }

  "amiResponseJson" - {
    "given a valid response" - {
      val response = mock[WSResponse]
      val json = Json.parse(AMIs.validAmiResponse)
      when(response.json).thenReturn(json)

      "returns the JsValue" in {
        amiResponseJson(response).awaitEither.right.value shouldEqual Json
          .parse(AMIs.validAMI)
      }
    }
  }

  "amisResponseJson" - {
    "given a valid response" - {
      val response = mock[WSResponse]
      val json = Json.parse(AMIs.validAmisResponse)
      when(response.json).thenReturn(json)

      "returns the JsValue" in {
        amisResponseJson(response).awaitEither.right.value shouldEqual List(
          Json.parse(AMIs.validAMI)
        )
      }
    }
  }

  "extractInstance" - {
    "should return an Instance from correct json" in {
      val json = Json.parse(Instances.validInstance)
      val instance = extractInstance(json).awaitEither.right.value
      instance should have(
        'name("instance-name"),
        'arn("arn:aws:ec2:region:0123456789:instance/i-id"),
        'stack(Some("stack")),
        'app(List("app")),
        'stage(Some("STAGE"))
      )
      instance.specification.get("imageId").value shouldEqual "ami-id"
    }

    "should return a failure given invalid json" in {
      val jsonStr = """{"testyinvalid":123}"""
      val json = Json.parse(jsonStr)
      extractInstance(json).awaitEither.isLeft shouldBe true
    }
  }

  "extractLaunchConfiguration" - {
    "should return a Launch Configuration from correct json" in {
      val json = Json.parse(LaunchConfigs.validLaunchConfiguration)
      val launchConfig =
        extractLaunchConfiguration(json).awaitEither.right.value
      launchConfig should have(
        'name("LaunchConfig-123"),
        'arn(
          "arn:aws:autoscaling:us-west-1:12343425:launchConfiguration:123-123-325-d121:launchConfigurationName/LaunchConfig-123"
        ),
        'imageId("ami-12345"),
        'region("us-west-1"),
        'createdTime(new DateTime(2015, 1, 2, 17, 0)),
        'instanceType("t2.micro"),
        'keyName("KeyPair")
      )
      launchConfig.securityGroups.head shouldEqual "arn:aws:ec2:us-west-1:12343243:security-group/sg-aa111111"
      launchConfig.meta.href shouldEqual "http://localhost:8080/configs/arn:aws:autoscaling:us-west-1:12343425:launchConfiguration:123-123-325-d121:launchConfigurationName:LaunchConfig-1233"
    }

    "should return a failure given invalid json" in {
      val jsonStr = """{"testyinvalid":123}"""
      val json = Json.parse(jsonStr)
      extractLaunchConfiguration(json).awaitEither.isLeft shouldBe true
    }
  }

  "extractAccounts" - {
    "should return an accounts list from correct json" in {
      val json = Json.parse(Accounts.validAccount)
      val accounts = extractAccounts(json).awaitEither.right.value
      accounts.accountName shouldEqual ("barnard-castle")
    }

    "should return a failure given invalid json" in {
      val jsonStr = """{"testyinvalid":123}"""
      val json = Json.parse(jsonStr)
      extractAccounts(json).awaitEither.isLeft shouldBe true
    }
  }
}
