package prism

import org.scalatest.mock.MockitoSugar
import org.scalatest.{EitherValues, Matchers, FreeSpec}
import PrismClient._
import play.api.libs.ws.WSResponse
import util.Fixtures._
import play.api.libs.json._
import org.mockito.Mockito._


class PrismClientTest extends FreeSpec with Matchers with EitherValues with MockitoSugar {
  "extractAMI" - {
    "should return an AMI from correct json" in {
      val json = Json.parse(JSON.validAMI)
      val ami = extractAMI(json).right.value
      ami should have(
        'name (Some("ami-name")),
        'arn ("arn:aws:ec2:region::image/ami-example")
      )
    }

    "should return a failure given invalid json" in {
      val jsonStr = """{"testyinvalid":123}"""
      val json = Json.parse(jsonStr)
      extractAMI(json).left.value
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
      val json = Json.parse(JSON.validAmiResponse)
      when(response.json).thenReturn(json)

      "returns the JsValue" in {
        amiResponseJson(response).right.value shouldEqual Json.parse(JSON.validAMI)
      }
    }
  }

  "amisResponseJson" - {
    "given a valid response" - {
      val response = mock[WSResponse]
      val json = Json.parse(JSON.validAmisResponse)
      when(response.json).thenReturn(json)

      "returns the JsValue" in {
        amisResponseJson(response).right.value shouldEqual List(Json.parse(JSON.validAMI))
      }
    }
  }
}
