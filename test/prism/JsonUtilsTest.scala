package prism

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
