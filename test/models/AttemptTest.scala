package models

import org.scalatest.{FreeSpec, Matchers}
import util.AttemptValues
import scala.concurrent.ExecutionContext

class AttemptTest extends FreeSpec with Matchers with AttemptValues {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  "WithFilter" - {
    val errors: AMIableErrors = AMIableErrors(AMIableError("error", "This is an error message", 515))
    val a1: Attempt[Int] = Attempt.Right(1)
    val attemptWithError: Attempt[Int] = Attempt.Left(errors)

    "should return an attempt with same value when it satisfies the predicate" in {
      a1.withFilter(_ == 1).awaitEither should equal(Right(1))
    }

    "should return an attempt with an error when value does NOT satisfy the predicate" in {
      a1.withFilter(_ > 1).awaitEither match {
        case Left(e) => // Expected result: doing nothing
        case _ => fail("should have failed")
      }
    }

    "should return an attempt with current errors when attempt fails" in {
      attemptWithError.withFilter(_ > 1).awaitEither should equal(Left(errors))
    }
  }

}
