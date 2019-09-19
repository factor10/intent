package intent.runner

import intent.{TestSuite, State, Stateless}
import intent.core.{ExpectationResult, TestError, TestFailed, Subscriber, TestCaseResult}
import intent.runner.{TestSuiteRunner, TestSuiteError, TestSuiteResult}
import intent.testdata._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.concurrent.Await

class TestSuiteRunnerTest extends TestSuite with State[TestSuiteTestCase]:
  "TestSuiteRunner" using TestSuiteTestCase() to :

    "running a stateful suite without context" using (_.noContextTestSuite) to :
      "has an error event" in:
        state =>
          whenComplete(state.runWithEventSubscriber()) :
            case Left(_) => fail("unexpected Left")
            case Right(_) =>
              val maybeMsg = state.receivedEvents().collectFirst { case TestCaseResult(_, _, TestError(msg, _)) => msg }
              maybeMsg match
                case Some(msg) => expect(msg).toMatch("^Top-level test cases".r)
                case None => fail("unexpected None")

    "running a suite that fails in setup" using (_.setupFailureTestSuite) to :
      "reports that 1 test was run" in :
        state =>
          whenComplete(state.runAll()) :
            case Left(_) => fail("unexpected Left")
            case Right(result) => expect(result.total).toEqual(1)

      "reports that 1 test failed" in:
        state =>
          whenComplete(state.runAll()) :
            case Left(_) => fail("unexpected Left")
            case Right(result) => expect(result.failed).toEqual(1)

      "has a failure event with the exception" in:
        state =>
          whenComplete(state.runWithEventSubscriber()) :
            case Left(_) => fail("unexpected Left")
            case Right(_) =>
              val maybeEx = state.receivedEvents().collectFirst { case TestCaseResult(_, _, TestFailed(_, Some(ex))) => ex }
              maybeEx match
                case Some(ex) => expect(ex.getMessage).toEqual("intentional failure")
                case None => fail("unexpected None")

    "running an empty suite" using (_.emptyTestSuite) to :
      "report that zero tests were run" in:
        state =>
          val possible = Await.result(state.runAll(), 5 seconds)
          possible match
            case Left(_) => fail("Unexpected Left")
            case Right(result) => expect(result.total).toEqual(0)  // TODO: Match on case class or individual fields?

    "running the OneOfEachResultTestSuite" using (_.oneOfEachResult) to :
      "report that totally 3 test was run" in:
        state =>
          val possible = Await.result(state.runAll(), 5 seconds)
          possible match
            case Left(_) => fail("Unexpected Left")
            case Right(result) => expect(result.total).toEqual(3)

      "report that 1 test was successful" in:
        state =>
          val possible = Await.result(state.runAll(), 5 seconds)
          possible match
            case Left(_) => fail("Unexpected Left")
            case Right(result) => expect(result.successful).toEqual(1)

      "report that 1 test was failed" in:
        state =>
          val possible = Await.result(state.runAll(), 5 seconds)
          possible match
            case Left(_) => fail("Unexpected Left")
            case Right(result) => expect(result.failed).toEqual(1)

      "report that 1 test had errors" in:
        state =>
          val possible = Await.result(state.runAll(), 5 seconds)
          possible match
            case Left(_) => fail("Unexpected Left")
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
  def setupFailureTestSuite = TestSuiteTestCase("intent.runner.StatefulFailingTestSuite")
  def noContextTestSuite = TestSuiteTestCase("intent.runner.StatefulNoContextTestSuite")

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
  "successful" in success()
  "failed" in fail("test should fail")
  "error" in:
    throw new RuntimeException("test should fail")

case class StatefulFailingTestState():
    def fail: StatefulFailingTestState =
      throw new RuntimeException("intentional failure")
class StatefulFailingTestSuite extends State[StatefulFailingTestState]:
  "root" using (StatefulFailingTestState()) to :
    "uh oh" using (_.fail) to :
      "won't get here" in :
        _ => expect(1).toEqual(2)

class StatefulNoContextTestSuite extends State[StatefulFailingTestState]:
  "won't get here" in :
    _ => expect(1).toEqual(2)
      