package intent.sbt
import intent.{Intent,TestPassed, TestFailed, TestError}

import sbt.testing.{
  Framework => SFramework,
  _}

class Framework extends SFramework {
  def name(): String = "intent"
  def fingerprints(): Array[Fingerprint] = Array(IntentFingerprint)

  def runner(args: Array[String], remoteArgs: Array[String], testClassLoader: ClassLoader): Runner =
    new SbtRunner(args, remoteArgs, testClassLoader)
}

/**
 * Defines how to find the Intent's test classes
 */
object IntentFingerprint extends SubclassFingerprint {
  // Disable the usage of modules (singleton objects) - this makes discovery faster
  val isModule = false

  // Constructor parameters will not be injected and will only cause confusion - disable.
  def requireNoArgConstructor(): Boolean = true

  // All tests needs to inherit from this type
  def superclassName(): String = "intent.IntentMaker"
}

/**
 * A single run of a single test (controlled by SBT)
 */
class SbtRunner(
  val args: Array[String],
  val remoteArgs: Array[String],
  classLoader: ClassLoader) extends Runner {

  def done(): String = {
    // This is called when test is done, and after that the test task myst not be called
    // Not obvious in the doc how what you're supposed to do here really..
    ""
  }

  def tasks(list: Array[TaskDef]): Array[Task] = list.map(SbtTask(_, classLoader))
}

/**
 * Top-level task runner, which in our case instantiates and loads a **Test Suite**
 * and run all the contained tests in there.
 *
 * Currently SBT feedback is only the name of the failed suite and not the specific
 * test.
 */
class SbtTask(td: TaskDef, classLoader: ClassLoader) extends Task {
  import scala.concurrent.{Await, Future}
  import scala.concurrent.duration._
  import scala.concurrent.ExecutionContext
  import scala.util.{Success, Failure}

  override def taskDef = td

  // Tagging tests or suites are currently not supported
  override def tags(): Array[String] = Array.empty

  override def execute(handler: EventHandler, loggers: Array[Logger]): Array[Task] = {
    implicit val ec = ExecutionContext.global
    // Hmm, do we need really to block here? Does SBT come with something included to be async
    Await.result(executeSuite(handler, loggers), Duration.Inf)
  }

  private def executeSuite(handler: EventHandler, loggers: Array[Logger]) given (ec: ExecutionContext): Future[Array[Task]] = {
    val testSuiteClass = classLoader.loadClass(td.fullyQualifiedName)
    val testSuite = testSuiteClass.newInstance.asInstanceOf[Intent[_]]
    val futureResults = testSuite.allTestCases.map(tc => {
      val beforeTime = System.currentTimeMillis
      def executionTime: Long = System.currentTimeMillis - beforeTime
      def testName: String = taskDef.fullyQualifiedName + " / " + tc.nameParts.mkString(" / ")
      def logPassed() = loggers.foreach(logger => logger.info(Console.GREEN + s"[PASSED] ${testName} (${executionTime} ms)"))
      def logFailed() = loggers.foreach(logger => logger.info(Console.RED + s"[FAILED] ${testName} (${executionTime} ms)"))

      val futureTestResult = tc.run()
      futureTestResult.onComplete {
        case Success(result) if result.expectationResult.isInstanceOf[TestPassed] =>
          handler.handle(SuccessfulEvent(executionTime, testName, taskDef.fingerprint))
          logPassed()

        case Success(result) if result.expectationResult.isInstanceOf[TestFailed] =>
          handler.handle(FailedEvent(executionTime, testName, taskDef.fingerprint))
          logFailed()

        case Success(result) if result.expectationResult.isInstanceOf[TestError] =>
          println(s"Test is broken: ${result.expectationResult}")

        case Failure(t) =>
          println(s"Test unexpectedly failed..${t}")
      }
      futureTestResult
    })

    Future.sequence(futureResults).map(_ => Array.empty)
  }
}

case class SuccessfulEvent(duration: Long, fullyQualifiedName: String, fingerprint: Fingerprint) extends Event {
  override def status = sbt.testing.Status.Success
  override def selector(): sbt.testing.Selector = sbt.testing.SuiteSelector()
  override def throwable(): sbt.testing.OptionalThrowable = sbt.testing.OptionalThrowable()
}

case class FailedEvent(duration: Long, fullyQualifiedName: String, fingerprint: Fingerprint) extends Event {
  override def status = sbt.testing.Status.Failure
  override def selector(): sbt.testing.Selector = sbt.testing.SuiteSelector()
  override def throwable(): sbt.testing.OptionalThrowable = sbt.testing.OptionalThrowable()
}
