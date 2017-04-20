package models

import models.Attempt.{Left, Right}
import org.scalatest._
import util.AttemptValues

import scala.concurrent.ExecutionContext.Implicits.global


class AttemptTest extends FreeSpec with Matchers with EitherValues with AttemptValues {
  "traverse" - {
    "returns the first failure" in {
      def failOnFourAndSix(i: Int): Attempt[Int] = {
        i match {
          case 4 => expectedFailure("fails on four")
          case 6 => expectedFailure("fails on six")
          case n => Right(n)
        }
      }
      val errors = Attempt.traverse(List(1, 2, 3, 4, 5))(failOnFourAndSix).awaitEither.left.value
      checkError(errors, "fails on four")
    }

    "returns the successful result if there were no failures" in {
      Attempt.traverse(List(1, 2, 3, 4))(Right).awaitEither.right.value shouldEqual List(1, 2, 3, 4)
    }
  }

  "successfulAttempts" - {
    "returns the list if all were successful" in {
      val attempts = List(Right(1), Right(2))

      Attempt.successfulAttempts(attempts).awaitEither.right.value shouldEqual List(1, 2)
    }

    "returns only the successful attempts if there were failures" in {
      val attempts: List[Attempt[Int]] = List(Right(1), Right(2), expectedFailure("failed"), Right(4))

      Attempt.successfulAttempts(attempts).awaitEither.right.value shouldEqual List(1, 2, 4)
    }
  }

  def checkError(errors: AMIableErrors, expected: String): Unit = {
    errors.errors.head.message shouldEqual expected
  }
  def expectedFailure[A](message: String): Attempt[A] = Left[A](AMIableError(message, "this will fail", 500))
}
