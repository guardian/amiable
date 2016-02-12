package prism

import org.scalatest.mock.MockitoSugar
import org.scalatest.{OptionValues, EitherValues, Matchers, FreeSpec}
import PrismClient._
import play.api.libs.ws.WSResponse
import util.Fixtures._
import play.api.libs.json._
import org.mockito.Mockito._


class PrismClientTest extends FreeSpec with Matchers with EitherValues with OptionValues with MockitoSugar {
  "extractAMI" - {
    "should return an AMI from correct json" in {
      val json = Json.parse(AMIs.validAMI)
      val ami = extractAMI(json).right.value
      ami should have(
        'name (Some("ami-name")),
        'arn ("arn:aws:ec2:region::image/ami-example")
      )
    }

    "should return a failure given invalid json" in {
      val jsonStr = """{"testyinvalid":123}"""
      val json = Json.parse(jsonStr)
      extractAMI(json).isLeft shouldBe true
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
        amiResponseJson(response).right.value shouldEqual Json.parse(AMIs.validAMI)
      }
    }
  }

  "amisResponseJson" - {
    "given a valid response" - {
      val response = mock[WSResponse]
      val json = Json.parse(AMIs.validAmisResponse)
      when(response.json).thenReturn(json)

      "returns the JsValue" in {
        amisResponseJson(response).right.value shouldEqual List(Json.parse(AMIs.validAMI))
      }
    }
  }

  "extractInstance" - {
    "should return an Instance from correct json" in {
      val json = Json.parse(Instances.validInstance)
      val instance = extractInstance(json).right.value
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
      extractInstance(json).isLeft shouldBe true
    }
  }

  "instancesUrl" - {
    val root = "http://root"

    "should contain the stack as a GET variable" in {
      instancesUrl("stack", "stage", "app", root) should include("stack=stack")
    }

    "should contain the stage as a GET variable" in {
      instancesUrl("stack", "stage", "app", root) should include("stage=stage")
    }

    "should contain the app as a GET variable" in {
      instancesUrl("stack", "stage", "app", root) should include("app=app")
    }
  }
}
