package intent.helpers

import scala.concurrent.{ExecutionContext, Future}
import intent.core._
import intent.runner.{TestSuiteRunner, TestSuiteError, TestSuiteResult}

/**
* Supports running a test suite and checking the result.
*/
trait TestSuiteRunnerTester(using ec: ExecutionContext) extends Subscriber[TestCaseResult]:

  def suiteClassName: String

  private object lock
  val runner = new TestSuiteRunner(cl)
  var events = List[TestCaseResult]()

  def runAll(): Future[Either[TestSuiteError, TestSuiteResult]] =
    assert(suiteClassName != null, "Suite class name must be set")
    runner.runSuite(suiteClassName)

  def runWithEventSubscriber(): Future[Either[TestSuiteError, TestSuiteResult]] =
    assert(suiteClassName != null, "Suite class name must be set")
    runner.runSuite(suiteClassName, Some(this))

  def evaluate(): IntentStructure = runner.evaluateSuite(suiteClassName).fold(_ => ???, identity)

  def receivedEvents(): Seq[TestCaseResult] = events

  override def onNext(event: TestCaseResult): Unit =
    lock.synchronized:
      events :+= event

  private def cl = getClass.getClassLoader
