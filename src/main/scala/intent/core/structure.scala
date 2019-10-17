package intent.core

import scala.concurrent.duration._
import scala.concurrent.Future

abstract class TestSuite {}

trait Expectation:
  private[intent] def evaluate(): Future[ExpectationResult]

sealed trait ExpectationResult
case class TestPassed() extends ExpectationResult

/**
  * TestFailed is used for errors happening once we have started to execute a test case.
  * This includes assertion errors/failures.
  *
  * @param output the failure output
  * @param ex optional exception, e.g. if some test state setup failed
  */
case class TestFailed(output: String, ex: Option[Throwable]) extends ExpectationResult

/**
  * TestError is used for errors happening before we can start executing a test case.
  *
  * @param context information about the error
  * @param ex the optional exception behind the error
  */
case class TestError(context: String, ex: Option[Throwable]) extends ExpectationResult

/**
 * TestIgnored is used to manually mark a test to not be evaluated.
 */
case class TestIgnored() extends ExpectationResult

case class TestCaseResult(duration: FiniteDuration, qualifiedName: Seq[String], expectationResult: ExpectationResult)

trait ITestCase:
  def nameParts: Seq[String]
  def run(): Future[TestCaseResult]
