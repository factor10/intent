package intent.runner

import intent.{TestSuite, Intent, Eq, Formatter, ExpectationResult, TestError}
import intent.runner.{TestSuiteRunner, TestSuiteError, TestSuiteResult}
import intent.testdata._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.concurrent.Await

class TestSuiteRunnerTest extends TestSuite with Intent[TestSuiteTestCase] {
  "TestSuiteRunner" - {

    "running an empty suite" - {
      "report that zero tests were run" in { state =>
        val possible = Await.result(state.runAll(), 5 seconds)
        possible match {
          case Left(_) => expect(false).toEqual(true)
          case Right(result) => expect(result.total).toEqual(0)  // TODO: Match on case class or individual fields?
        }
      }
    }

    "running the OneOfEachResultTestSuite" via oneOfEachResult - {
      "report that totally 3 test was run" in { state =>
        val possible = Await.result(state.runAll(), 5 seconds)
        possible match {
          case Left(_) => expect(false).toEqual(true)
          case Right(result) => expect(result.total).toEqual(3)
        }
      }

      "report that 1 test was successful" in { state =>
        val possible = Await.result(state.runAll(), 5 seconds)
        possible match {
          case Left(_) => expect(false).toEqual(true)
          case Right(result) => expect(result.successful).toEqual(1)
        }
      }

      "report that 1 test was failed" in { state =>
        val possible = Await.result(state.runAll(), 5 seconds)
        possible match {
          case Left(_) => expect(false).toEqual(true)
          case Right(result) => expect(result.failed).toEqual(1)
        }
      }

      "report that 1 test had errors" in { state =>
        val possible = Await.result(state.runAll(), 5 seconds)
        possible match {
          case Left(_) => expect(false).toEqual(true)
          case Right(result) => expect(result.errors).toEqual(1)
        }
      }
    }

    "when test suite cannot be instantiated" via invalidTestSuiteClass - {
      "a TestSuiteError should be received" in { state =>
        // TOOD: Something better than `toCompleteWith` is needed when working with Futures.
        //       Maybe something similar to ScalaTest eventually / whenReady?

        val possible = Await.result(state.runAll(), 5 seconds)
        possible match {
          case Left(e) => expect(s"${e.ex.getClass}: ${e.ex.getMessage}").toEqual("class java.lang.ClassNotFoundException: foo.Bar")
          case Right(_) => expect(false).toEqual(true)
        }
      }
    }
  }

  def emptyState = TestSuiteTestCase("intent.testdata.EmtpyTestSuite")
  def invalidTestSuiteClass(initial: TestSuiteTestCase) = TestSuiteTestCase("foo.Bar")
  def oneOfEachResult(initial: TestSuiteTestCase) = TestSuiteTestCase("intent.runner.OneOfEachResultTestSuite")
}

/**
 * Wraps a runner for a specific test suite
 */
case class TestSuiteTestCase(suiteClassName: String) given (ec: ExecutionContext) {
  val runner = new TestSuiteRunner(cl)

  def runAll(): Future[Either[TestSuiteError, TestSuiteResult]] = runner.runSuite(suiteClassName)

  private def cl = getClass.getClassLoader
}

class OneOfEachResultTestSuite extends Intent[Unit] {
    "successful" in (state => expect(true).toEqual(true))
    "failed" in (state => expect(true).toEqual(false))
    "error" in (state => throw new RuntimeException("test should fail"))
  def emptyState = ()
}