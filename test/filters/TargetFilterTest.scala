package filters

import org.apache.pekko.stream.Materializer
import org.mockito.Mockito.mock
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import play.api.mvc.{RequestHeader, Results}

import scala.concurrent.Future

class TargetFilterTest extends AsyncFreeSpec with Matchers {
  private implicit val materializer: Materializer =
    mock(classOf[Materializer])
  private val request = mock(classOf[RequestHeader])

  "TargetFilter" - {
    "labels Fargate responses and preserves the existing response" in {
      val filter = new TargetFilter(
        Map("AWS_EXECUTION_ENV" -> "AWS_ECS_FARGATE")
      )
      val response = Results.Ok("body").withHeaders("Existing" -> "value")

      filter(_ => Future.successful(response))(request).map { result =>
        result.header.headers("X-Gu-Target") shouldEqual "ecs"
        result.header.headers("Existing") shouldEqual "value"
        result.header.status shouldEqual 200
        result.body shouldEqual response.body
      }
    }

    "labels ECS on EC2 responses as ecs" in {
      val filter = new TargetFilter(
        Map("AWS_EXECUTION_ENV" -> "AWS_ECS_EC2")
      )

      filter(_ => Future.successful(Results.Ok))(request).map { result =>
        result.header.headers("X-Gu-Target") shouldEqual "ecs"
      }
    }

    "defaults unrecognised execution environments to ec2" in {
      val filter = new TargetFilter(
        Map("AWS_EXECUTION_ENV" -> "unrecognised")
      )

      filter(_ => Future.successful(Results.Ok))(request).map { result =>
        result.header.headers("X-Gu-Target") shouldEqual "ec2"
      }
    }

    "labels non-ECS responses as ec2, including redirects" in {
      val filter = new TargetFilter(Map.empty)

      filter(_ => Future.successful(Results.Redirect("/login")))(request)
        .map { result =>
          result.header.headers("X-Gu-Target") shouldEqual "ec2"
          result.header.headers("Location") shouldEqual "/login"
          result.header.status shouldEqual 303
        }
    }
  }
}
