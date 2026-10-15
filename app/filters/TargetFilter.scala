package filters

import org.apache.pekko.stream.Materializer
import play.api.mvc.{Filter, RequestHeader, Result}

import scala.concurrent.{ExecutionContext, Future}

class TargetFilter(environment: Map[String, String])(implicit
    val mat: Materializer,
    ec: ExecutionContext
) extends Filter {
  private val target =
    environment.get("AWS_EXECUTION_ENV") match {
      case Some("AWS_ECS_FARGATE" | "AWS_ECS_EC2") => "ecs"
      case _                                       => "ec2"
    }

  override def apply(
      next: RequestHeader => Future[Result]
  )(request: RequestHeader): Future[Result] =
    next(request).map(_.withHeaders("X-Gu-Target" -> target))
}
