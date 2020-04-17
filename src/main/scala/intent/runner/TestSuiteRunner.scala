package intent.runner

import scala.collection.immutable.Stream
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Try,Success,Failure}

import intent.core.{IntentStructure, ExpectationResult, TestSuite, TestCaseResult,
  TestPassed, TestFailed, TestError, TestIgnored, Subscriber}

/**
 * Fatal error during construction or loading (including test discovery) of a test suite.
 */
case class TestSuiteError(ex: Throwable) extends Throwable

/**
 * A successful test suite result.
 *
 * @param total The total number of tests run
 * @param successful The number of successful tests
 * @param failed The number of failed tests (when assertion failed)
 * @param errors The number of tests that caused error (exception, not failed assertion)
 * @param ignored The number of tests that are ignored (not run)
 */
case class TestSuiteResult(total: Int = 0, successful: Int = 0, failed: Int = 0, errors: Int = 0, ignored: Int = 0):
  def incSuccess(): TestSuiteResult = this.copy(
      total = total + 1,
      successful = successful + 1)

  def incFailure(): TestSuiteResult = this.copy(
      total = total + 1,
      failed = failed + 1)

  def incError(): TestSuiteResult = this.copy(
      total = total + 1,
      errors = errors + 1)

  def incIgnore(): TestSuiteResult = this.copy(
    total = total + 1,
    ignored = ignored +1)

/**
 * A test suite runner.
 *
 * The runner will not load any suites until a specific class is requested to be run. The runner does not
 * keep state between runs so it is safe to reuse the runner for multiple suites if perferred.
 *
 * @param classLoader The class loader used to load and instantiate the test suite
 */
class TestSuiteRunner(classLoader: ClassLoader):
  /**
   * Instantiate and run the given test suite.
   *
   * The method will eventually resolve with either a [[TestSuiteResult]] upon success, else [[TestSuiteError]] will
   * be returned with details on the error that occured.
   *
   * Until the full suite is executed a subscriber can be given to receive events during the test-run. A custom
   * subscriber is also recommended if more details than the successful result is needed.
   */
  def runSuite(className: String, eventSubscriber: Option[Subscriber[TestCaseResult]] = None)(using ec: ExecutionContext): Future[Either[TestSuiteError, TestSuiteResult]] =
    instantiateSuite(className) match
      case Success(instance) => runTestsForSuite(instance, eventSubscriber).map(res => Right(res))
      case Failure(ex: Throwable) => Future.successful(Left(TestSuiteError(ex)))

  /**
   * Instantiate the given test suite and evaluate which tests that should be run or ignored
   */
  private[intent] def evaluateSuite(className: String): Either[TestSuiteError, IntentStructure] =
    instantiateSuite(className) match
      case Success(instance) => Right(instance)
      case Failure(ex: Throwable) => Left(TestSuiteError(ex))

  private def runTestsForSuite(suite: IntentStructure, eventSubscriber: Option[Subscriber[TestCaseResult]])(using ec: ExecutionContext): Future[TestSuiteResult] =
    // TODO: We should measure Suite time as well. Might be good to find expensive setup or scheduling problems.
    val futureTestResults = suite.allTestCases.map(tc =>
      eventSubscriber match {
        case Some(subscriber) =>
          val promise = Promise[TestCaseResult]()
          tc.run().onComplete:
            case Success(result) =>
              subscriber.onNext(result)
              promise.success(result)
            case Failure(ex) =>
              // TODO: Should we wrap the error in the result or should we add `onError()`?
              promise.failure(ex)
          promise.future
        case None => tc.run()
      })

    // Aggregate result now that all tests are run
    Future.sequence(futureTestResults).map(all => all.foldLeft(TestSuiteResult())((acc: TestSuiteResult, res: TestCaseResult)  => {
      res.expectationResult match
        case success: TestPassed => acc.incSuccess()
        case failure: TestFailed => acc.incFailure()
        case error: TestError => acc.incError()
        case ignored: TestIgnored => acc.incIgnore()
        case null => throw new IllegalStateException("Unsupported test result: null")
    }))

  private def instantiateSuite(className: String): Try[IntentStructure] =
    Try(classLoader.loadClass(className)
      .getDeclaredConstructor()
      .newInstance().asInstanceOf[IntentStructure])
