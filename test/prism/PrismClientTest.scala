package prism

import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{EitherValues, FreeSpec, Matchers, OptionValues}
import play.api.libs.ws.WSResponse
import prism.PrismClient._
import util.AttemptValues
import util.Fixtures._
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global


class PrismClientTest extends FreeSpec with Matchers with EitherValues with AttemptValues with OptionValues with MockitoSugar {
  "extractAMI" - {
    "should return an AMI from correct json" in {
      val json = Json.parse(AMIs.validAMI)
      val ami = extractAMI(json).awaitEither.right.value
      ami should have(
        'name (Some("ami-name")),
        'arn ("arn:aws:ec2:region::image/ami-example")
      )
    }

    "should return a failure given invalid json" in {
      val jsonStr = """{"testyinvalid":123}"""
      val json = Json.parse(jsonStr)
      extractAMI(json).awaitEither.isLeft shouldBe true
    }
  }

  "amiUrl" - {
    "should generate this url" in {
      amiUrl("ami-blah", "http://root") shouldEqual "http://root/images/ami-blah"
    }

    "should url encode the arn" in {
      amiUrl("ami/blah", "") should include("ami%2Fblah")
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
        'stack ("stack"),
        'app (List("app")),
        'stage ("STAGE")
      )
      instance.specification.get("imageId").value shouldEqual "ami-id"
    }

    "should return a failure given invalid json" in {
      val jsonStr = """{"testyinvalid":123}"""
      val json = Json.parse(jsonStr)
      extractInstance(json).awaitEither.isLeft shouldBe true
    }
  }

  "instancesUrl" - {
    val root = "http://root"

    "should contain the stack as a GET variable" in {
      instancesUrl(Some("stack"), None, None, root) should include("stack=stack")
    }

    "should contain the stage as a GET variable" in {
      instancesUrl(None, Some("stage"), None, root) should include("stage=stage")
    }

    "should contain the app as a GET variable" in {
      instancesUrl(None, None, Some("app"), root) should include("app=app")
    }

    "should not contain an app parameter if no app is provided" in {
      instancesUrl(Some("stack"), Some("stage"), None, root) shouldNot include("app=app")
    }

    "should not contain a stage parameter if no stage is provided" in {
      instancesUrl(Some("stack"), None, Some("app"), root) shouldNot include("stage=stage")
    }

    "should not contain a stack parameter if no stack is provided" in {
      instancesUrl(None, Some("stage"), Some("app"), root) shouldNot include("stack=stack")
    }

    "uses the instances path" in {
      instancesUrl(None, None, None, root) should startWith(s"$root/instances?")
    }
  }
}
