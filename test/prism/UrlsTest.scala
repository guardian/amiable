package prism

import org.scalatest.mock.MockitoSugar
import org.scalatest.{EitherValues, FreeSpec, Matchers, OptionValues}
import prism.Urls._
import util.AttemptValues


class UrlsTest extends FreeSpec with Matchers with EitherValues with AttemptValues with OptionValues with MockitoSugar {
  "amiUrl" - {
    "should generate this url" in {
      amiUrl("ami-blah", "http://root") shouldEqual "http://root/images/ami-blah"
    }

    "should url encode the arn" in {
      amiUrl("ami/blah", "") should include("ami%2Fblah")
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

    "ignores empty stage" in {
      instancesUrl(Some(""), None, None, root) shouldNot include(s"stage=")
    }

    "ignores empty stack" in {
      instancesUrl(Some(""), None, None, root) shouldNot include(s"stack=")
    }

    "ignores empty app" in {
      instancesUrl(Some(""), None, None, root) shouldNot include(s"app=")
    }
  }

  "emptyToNone" - {
    "returns None for None" in {
      emptyToNone(None) shouldEqual None
    }

    "returns None for empty string" in {
      emptyToNone(Some("")) shouldEqual None
    }

    "returns Some for non-empty string" in {
      emptyToNone(Some("abc")) shouldEqual Some("abc")
    }
  }
}
