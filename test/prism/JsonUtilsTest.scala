package prism

import org.joda.time.DateTime
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{EitherValues, FreeSpec, Matchers, OptionValues}
import play.api.libs.ws.WSResponse
import util.AttemptValues
import util.Fixtures._
import play.api.libs.json._
import prism.JsonUtils._

import scala.concurrent.ExecutionContext.Implicits.global


class JsonUtilsTest extends FreeSpec with Matchers with EitherValues with AttemptValues with OptionValues with MockitoSugar {
  "extractAMI" - {
    "should return an AMI from correct json" in {
      val json = Json.parse(AMIs.validAMI)
      val ami = extractAMI(json).awaitEither.right.value
      ami should have(
        'arn ("arn:aws:ec2:region::image/ami-example"),
        'name (Some("ami-name")),
        'imageId ("ami-example"),
        'region ("region"),
        'description (Some("AMI description")),
        'creationDate (Some(new DateTime(2016, 2, 11, 0, 0))),
        'state ("available"),
        'architecture ("x86_64"),
        'ownerId ("0123456789"),
        'virtualizationType ("hvm"),
        'hypervisor ("xen"),
        'rootDeviceType ("ebs"),
        'sriovNetSupport (Some("simple")),
        'upgrade (None)
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
        amiResponseJson(response).awaitEither.right.value shouldEqual Json.parse(AMIs.validAMI)
      }
    }
  }

  "amisResponseJson" - {
    "given a valid response" - {
      val response = mock[WSResponse]
      val json = Json.parse(AMIs.validAmisResponse)
      when(response.json).thenReturn(json)

      "returns the JsValue" in {
        amisResponseJson(response).awaitEither.right.value shouldEqual List(Json.parse(AMIs.validAMI))
      }
    }
  }

  "extractInstance" - {
    "should return an Instance from correct json" in {
      val json = Json.parse(Instances.validInstance)
      val instance = extractInstance(json).awaitEither.right.value
      instance should have(
        'name ("instance-name"),
        'arn ("arn:aws:ec2:region:0123456789:instance/i-id"),
        'stack (Some("stack")),
        'app (List("app")),
        'stage (Some("STAGE"))
      )
      instance.specification.get("imageId").value shouldEqual "ami-id"
    }

    "should return a failure given invalid json" in {
      val jsonStr = """{"testyinvalid":123}"""
      val json = Json.parse(jsonStr)
      extractInstance(json).awaitEither.isLeft shouldBe true
    }
  }
}
