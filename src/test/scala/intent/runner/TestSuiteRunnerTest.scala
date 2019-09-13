package intent.runner

import intent.{TestSuite, State, Stateless, Eq, Formatter}
import intent.core.{ExpectationResult, TestError, Subscriber, TestCaseResult}
import intent.runner.{TestSuiteRunner, TestSuiteError, TestSuiteResult}
import intent.testdata._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.concurrent.Await

class TestSuiteRunnerTest extends TestSuite with State[TestSuiteTestCase]:
  "TestSuiteRunner" using TestSuiteTestCase() to :

    "running an empty suite" using (_.emptyTestSuite) to :
      "report that zero tests were run" in:
        state =>
          val possible = Await.result(state.runAll(), 5 seconds)
          possible match
            case Left(_) => expect(false).toEqual(true)
            case Right(result) => expect(result.total).toEqual(0)  // TODO: Match on case class or individual fields?

    "running the OneOfEachResultTestSuite" using (_.oneOfEachResult) to :
      "report that totally 3 test was run" in:
        state =>
          val possible = Await.result(state.runAll(), 5 seconds)
          possible match
            case Left(_) => expect(false).toEqual(true)
            case Right(result) => expect(result.total).toEqual(3)

      "report that 1 test was successful" in:
        state =>
          val possible = Await.result(state.runAll(), 5 seconds)
          possible match
            case Left(_) => expect(false).toEqual(true)
            case Right(result) => expect(result.successful).toEqual(1)

      "report that 1 test was failed" in:
        state =>
          val possible = Await.result(state.runAll(), 5 seconds)
          possible match
            case Left(_) => expect(false).toEqual(true)
            case Right(result) => expect(result.failed).toEqual(1)

      "report that 1 test had errors" in:
        state =>
          val possible = Await.result(state.runAll(), 5 seconds)
          possible match
            case Left(_) => expect(false).toEqual(true)
            case Right(result) => expect(result.errors).toEqual(1)

      "with a registered event subscriber" using (_.copy()) to : // TODO: can we use identity here?
        "should publish 3 events" in:
          state =>
            val _ = Await.result(state.runWithEventSubscriber(), 5 seconds)
            expect(state.receivedEvents()).toHaveLength(3)

    "when test suite cannot be instantiated" using (_.invalidTestSuiteClass) to :
      "a TestSuiteError should be received" in:
        state =>
          // TOOD: Something better than `toCompleteWith` is needed when working with Futures.
          //       Maybe something similar to ScalaTest eventually / whenReady?

          val possible = Await.result(state.runAll(), 5 seconds)
          possible match
            case Left(e) => expect(s"${e.ex.getClass}: ${e.ex.getMessage}").toEqual("class java.lang.ClassNotFoundException: foo.Bar")
            case Right(_) => expect(false).toEqual(true)

/**
 * Wraps a runner for a specific test suite
 */
case class TestSuiteTestCase(suiteClassName: String = null) given (ec: ExecutionContext) extends Subscriber[TestCaseResult]:

  def emptyTestSuite = TestSuiteTestCase("intent.testdata.EmtpyTestSuite")
  def invalidTestSuiteClass = TestSuiteTestCase("foo.Bar")
  def oneOfEachResult = TestSuiteTestCase("intent.runner.OneOfEachResultTestSuite")

  private object lock
  val runner = new TestSuiteRunner(cl)
  var events = List[TestCaseResult]()

  def runAll(): Future[Either[TestSuiteError, TestSuiteResult]] =
    assert(suiteClassName != null, "Suite class name must be set")
    runner.runSuite(suiteClassName)
  def runWithEventSubscriber(): Future[Either[TestSuiteError, TestSuiteResult]] =
    assert(suiteClassName != null, "Suite class name must be set")
    runner.runSuite(suiteClassName, Some(this))
  def receivedEvents(): Seq[TestCaseResult] = events

  override def onNext(event: TestCaseResult): Unit =
    lock.synchronized:
      events :+= event

  private def cl = getClass.getClassLoader

class OneOfEachResultTestSuite extends Stateless :
  "successful" in expect(true).toEqual(true)
  "failed" in expect(true).toEqual(false)
  "error" in:
    throw new RuntimeException("test should fail")