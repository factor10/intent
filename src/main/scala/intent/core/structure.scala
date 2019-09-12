package intent.core

import scala.concurrent.duration._
import scala.concurrent.Future

class TestSuite {}

trait Expectation:
  private[intent] def evaluate(): Future[ExpectationResult]

sealed trait ExpectationResult
case class TestPassed() extends ExpectationResult
case class TestFailed(output: String) extends ExpectationResult
case class TestError(ex: Throwable) extends ExpectationResult

case class TestCaseResult(duration: FiniteDuration, qualifiedName: Seq[String], expectationResult: ExpectationResult)

trait ITestCase:
  def nameParts: Seq[String]
  def run(): Future[TestCaseResult]
