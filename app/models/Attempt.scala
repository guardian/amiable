package models

import play.api.mvc.Result

import scala.concurrent.{ExecutionContext, Future}


case class Attempt[A] private (underlying: Future[Either[AMIableErrors, A]]) {
  def map[B](f: A => B)(implicit ec: ExecutionContext): Attempt[B] =
    flatMap(a => Attempt.Right(f(a)))

  def flatMap[B](f: A => Attempt[B])(implicit ec: ExecutionContext): Attempt[B] = Attempt {
    asFuture.flatMap {
      case Right(a) => f(a).asFuture
      case Left(e) => Future.successful(Left(e))
    }
  }

  def fold[B](failure: AMIableErrors => B, success: A => B)(implicit ec: ExecutionContext): Future[B] = {
    asFuture.map(_.fold(failure, success))
  }

  /**
    * If there is an error in the Future itself (e.g. a timeout) we convert it to a
    * Left so we have a consistent error representation. This would likely have
    * logging around it, or you may have an error representation that carries more info
    * for these kinds of issues.
    */
  def asFuture(implicit ec: ExecutionContext): Future[Either[AMIableErrors, A]] = {
    underlying recover { case err =>
      val apiErrors = AMIableErrors(AMIableError(err.getMessage, "Unexpected error", 500))
      scala.Left(apiErrors)
    }
  }
}

object Attempt {
  def sequence[A](responses: List[Attempt[A]])(implicit ec: ExecutionContext): Attempt[List[A]] = Attempt {
    Future.sequence(responses.map(_.underlying)).map { eithers =>
      eithers
        .collectFirst { case scala.Left(x) => scala.Left(x): Either[AMIableErrors, List[A]]}
        .getOrElse {
          scala.Right(eithers collect { case Right(x) => x})
        }
    }
  }

  def fromEither[A](e: Either[AMIableErrors, A]): Attempt[A] =
    Attempt(Future.successful(e))

  def fromFuture[A](future: Future[Either[AMIableErrors, A]])(recovery: PartialFunction[Throwable, Either[AMIableErrors, A]])(implicit ec: ExecutionContext): Attempt[A] = {
    Attempt(future recover recovery)
  }

  /**
    * Create an Attempt instance from a "good" value.
    */
  def Right[A](a: A): Attempt[A] =
    Attempt(Future.successful(scala.Right(a)))

  /**
    * Create an Attempt failure from an AMIableErrors instance.
    */
  def Left[A](err: AMIableErrors): Attempt[A] =
    Attempt(Future.successful(scala.Left(err)))

  /**
    * Asyncronous versions of the Attempt Right/Left helpers for when you have
    * a Future that returns a good/bad value directly.
    */
  object Async {
    /**
      * Create an Attempt from a Future of a good value.
      */
    def Right[A](fa: Future[A])(implicit ec: ExecutionContext): Attempt[A] =
      Attempt(fa.map(scala.Right(_)))

    /**
      * Create an Attempt from a known failure in the future. For example,
      * if a piece of logic fails but you need to make a Database/API call to
      * get the failure information.
      */
    def Left[A](ferr: Future[AMIableErrors])(implicit ec: ExecutionContext): Attempt[A] =
      Attempt(ferr.map(scala.Left(_)))
  }

  def apply[A](action: => Attempt[Result])(errorHandler: AMIableErrors => Result)(implicit ec: ExecutionContext) = {
    action.fold(
      errorHandler,
      identity
    )
  }
}