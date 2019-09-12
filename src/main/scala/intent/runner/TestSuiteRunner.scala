package intent.runner

import scala.collection.immutable.Stream
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Try,Success,Failure}

import intent.{ExpectationResult, TestError, TestSuite, Intent, TestCaseResult}

/**
 * Fatal error during construction or loading (including test discovery) of a test suite.
 */
case class TestSuiteError(ex: Throwable) extends Throwable
/**
 * A successful test suite result.
 *
 * @param total The total number of tests run
 */
case class TestSuiteResult(total: Int)

/**
 * A test suite runner.
 *
 * The runner will not load any suites until a specific class is requested to be run. The runner does not
 * keep state between runs so it is safe to reuse the runner for multiple suites if perferred.
 *
 * @param classLoader The class loader used to load and instantiate the test suite
 */
class TestSuiteRunner(classLoader: ClassLoader) {
  /**
   * Instantiate and run the given test suite.
   *
   * The method will eventually resolve with either a [[TestSuiteResult]] upon success, else [[TestSuiteError]] will
   * be returned with details on the error that occured.
   *
   * Until the full suite is executed a subscriber can be given to receive events during the test-run. A custom
   * subscriber is also recommended if more details than the successful result is needed.
   */
  def runSuite(className: String) given(ec: ExecutionContext): Future[Either[TestSuiteError, TestSuiteResult]] = {
    Future {
      instantiateSuite(className) match {
        case Success(instance) => Right(TestSuiteResult(0))
        case Failure(ex: Throwable) => Left(TestSuiteError(ex))
      }
    }
  }

  private def instantiateSuite(className: String): Try[Intent[_]] =
    Try(classLoader.loadClass(className).newInstance.asInstanceOf[Intent[_]])
}
