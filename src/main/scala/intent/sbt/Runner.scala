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
  def superclassName(): String = "intent.TestSuite"
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
    // Whatever is returned here is printed in the SBT log just before the summary
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

    // TODO: Wrap this in a Try and report failure if a test cannot be loaded
    val testSuite = testSuiteClass.newInstance.asInstanceOf[Intent[_]]
    val futureResults = testSuite.allTestCases.map(tc => {
      val beforeTime = System.currentTimeMillis
      def executionTime: Long = System.currentTimeMillis - beforeTime

      val futureTestResult = tc.run()

      futureTestResult.onComplete {
        case Success(result) if result.expectationResult.isInstanceOf[TestPassed] =>
          val event = SuccessfulEvent(executionTime, taskDef.fullyQualifiedName, tc.nameParts, taskDef.fingerprint)
          handler.handle(event)
          event.log(loggers, executionTime)

        case Success(result) if result.expectationResult.isInstanceOf[TestFailed] =>
          val event = FailedEvent(executionTime, taskDef.fullyQualifiedName, tc.nameParts, taskDef.fingerprint, result.expectationResult.asInstanceOf[TestFailed].output)
          handler.handle(event)
          event.log(loggers, executionTime)

        case Success(result) if result.expectationResult.isInstanceOf[TestError] =>
          println(s"Test is broken: ${result.expectationResult}")
          val event = ErrorEvent(executionTime, taskDef.fullyQualifiedName, tc.nameParts, taskDef.fingerprint, result.expectationResult.asInstanceOf[TestError].ex)
          handler.handle(event)
          event.log(loggers, executionTime)

        case Failure(t) =>
          println(s"Test unexpectedly failed..${t}")
          val event = ErrorEvent(executionTime, taskDef.fullyQualifiedName, tc.nameParts, taskDef.fingerprint, t)
          handler.handle(event)
          event.log(loggers, executionTime)
      }

      futureTestResult
    })

    Future.sequence(futureResults).map(_ => Array.empty)
  }
}

trait LoggedEvent(color: String, prefix: String, suiteName: String, testNames: Seq[String]) {
  val fullyQualifiedTestName: String = suiteName + " >> " + testNames.mkString(" >> ")

  def log(loggers: Array[Logger], executionTime: Long): Unit = loggers.foreach(_.info(color + s"[${prefix}] ${fullyQualifiedTestName} (${executionTime} ms)"))
}

case class SuccessfulEvent(duration: Long, suiteName: String, testNames: Seq[String], fingerprint: Fingerprint) extends Event
  with LoggedEvent(Console.GREEN, "PASSED", suiteName, testNames) {

  override def fullyQualifiedName = suiteName
  override def status = sbt.testing.Status.Success
  override def selector(): sbt.testing.Selector = new NestedTestSelector(suiteName, testNames.mkString(" >> "))
  override def throwable(): sbt.testing.OptionalThrowable = sbt.testing.OptionalThrowable()
}

case class FailedEvent(duration: Long, suiteName: String, testNames: Seq[String], fingerprint: Fingerprint, assertionMessage: String) extends Event
  with LoggedEvent(Console.RED, "FAILED", suiteName, testNames) {
  val color = Console.RED // Why are not these inherited form LoggedEvent?
  val prefix = "ERROR"

  override def fullyQualifiedName = suiteName
  override def status = sbt.testing.Status.Failure
  override def selector(): sbt.testing.Selector = new NestedTestSelector(suiteName, testNames.mkString(" >> "))
  override def throwable(): sbt.testing.OptionalThrowable = sbt.testing.OptionalThrowable()

  override def log(loggers: Array[Logger], executionTime: Long): Unit =
    loggers.foreach(_.info(color + s"[${prefix}] ${fullyQualifiedTestName} (${executionTime} ms) \n\t${Console.RED}${assertionMessage}"))
}

case class ErrorEvent(duration: Long, suiteName: String, testNames: Seq[String], fingerprint: Fingerprint, err: Throwable) extends Event
  with LoggedEvent(Console.RED, "ERROR", suiteName, testNames) {
  val color = Console.RED // Why are not these inherited form LoggedEvent?
  val prefix = "ERROR"

  override def fullyQualifiedName = suiteName
  override def status = sbt.testing.Status.Failure
  override def selector(): sbt.testing.Selector = new NestedTestSelector(suiteName, testNames.mkString(" >> "))
  override def throwable(): sbt.testing.OptionalThrowable = sbt.testing.OptionalThrowable(err)
  override def log(loggers: Array[Logger], executionTime: Long): Unit =
    loggers.foreach(_.info(color + s"[${prefix}] ${fullyQualifiedTestName} (${executionTime} ms) \n\t${Console.RED}${err.getMessage}"))
}