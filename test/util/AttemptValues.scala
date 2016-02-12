package util

import models.Attempt

import scala.concurrent._
import scala.concurrent.duration._

trait AttemptValues {
  implicit class RichAttempt[A](attempt: Attempt[A]) {
    def awaitEither(implicit ec: ExecutionContext) =
      Await.result(attempt.asFuture, 5.seconds)
  }
}
