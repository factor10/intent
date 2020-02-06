package intent.sbt

import intent.core.{IntentStructure, TestPassed, TestFailed, TestError, TestIgnored, Subscriber, TestCaseResult}
import intent.runner.TestSuiteRunner
import sbt.testing.{Framework => SFramework, _ }
import scala.concurrent.duration._
import scala.language.implicitConversions
import java.io.{PrintWriter, StringWriter}

class Framework extends SFramework with
  def name(): String = "intent"
  def fingerprints(): Array[Fingerprint] = Array(IntentFingerprint)
  def runner(args: Array[String], remoteArgs: Array[String], testClassLoader: ClassLoader): Runner =
    new SbtRunner(args, remoteArgs, testClassLoader)

private def printErrorWithPrefix(t: Throwable, linePrefix: String): String =
  val sw = StringWriter()
  t.printStackTrace(new PrintWriter(sw))
  sw.toString.split("\n").map(line => linePrefix + line).mkString("\n")

/**
 * Defines how to find the Intent's test classes
 */
object IntentFingerprint extends SubclassFingerprint with
  // Disable the usage of modules (singleton objects) - this makes discovery faster
  val isModule = false

  // Constructor parameters will not be injected and will only cause confusion - disable.
  def requireNoArgConstructor(): Boolean = true

  // All tests needs to inherit from this type
  def superclassName(): String = "intent.core.TestSuite"

/**
 * A single run of a single test (controlled by SBT)
 */
class SbtRunner(
  val args: Array[String],
  val remoteArgs: Array[String],
  classLoader: ClassLoader
) extends Runner with

  def done(): String =
    // This is called when test is done, and after that the test task myst not be called
    // Whatever is returned here is printed in the SBT log just before the summary
    ""

  /**
   * Called by SBT with all the classes that match our fingerprint.
   *
   */
  def tasks(potentialTasks: Array[TaskDef]): Array[Task] =
    case class Suite(td: TaskDef, structure: IntentStructure)

    val runner = new TestSuiteRunner(classLoader)
    var focusMode = false
    var potentialSuites: Array[Suite] = potentialTasks
      .map(td => {
        runner.evaluateSuite(td.fullyQualifiedName) match {
          case Left(ex) =>
            // At this point there is no loggers, so just throw and abort execution.
            // Errors here might occur if the class cannot be found, or if an error
            // happen while instantiating
            throw ex
          case Right(suite) =>
            if suite.isFocused then focusMode = true
            Suite(td, suite)
        }
      })

    if focusMode then
      potentialSuites = potentialSuites.filter(_.structure.isFocused)

    potentialSuites
      .map(s => SbtTask(s.td, s.structure, runner, focusMode))

class SbtTask(td: TaskDef, suit: IntentStructure, runner: TestSuiteRunner, focusMode: Boolean) extends Task with
  import scala.concurrent.{Await, Future}
  import scala.concurrent.duration._
  import scala.concurrent.ExecutionContext
  import scala.util.{Success, Failure}

  override def taskDef = td

  // Tagging tests or suites are currently not supported
  override def tags(): Array[String] = Array.empty

  override def execute(handler: EventHandler, loggers: Array[Logger]): Array[Task] =
    implicit val ec = ExecutionContext.global
    // Hmm, do we need really to block here? Does SBT come with something included to be async
    Await.result(executeSuite(handler, loggers), Duration.Inf)

  private def executeSuite(handler: EventHandler, loggers: Array[Logger])(using ec: ExecutionContext): Future[Array[Task]] =
    object lock
    val eventSubscriber = new Subscriber[TestCaseResult]:
      override def onNext(event: TestCaseResult): Unit =
        val sbtEvent = event.expectationResult match
          case success: TestPassed  => SuccessfulEvent(event.duration.toMillis, taskDef.fullyQualifiedName, event.qualifiedName, taskDef.fingerprint, focusMode)
          case failure: TestFailed  => FailedEvent(event.duration.toMillis, taskDef.fullyQualifiedName, event.qualifiedName, taskDef.fingerprint, focusMode, failure.output, failure.ex)
          case error: TestError     => ErrorEvent(event.duration.toMillis, taskDef.fullyQualifiedName, event.qualifiedName, taskDef.fingerprint, focusMode, error.context, error.ex)
          case ignored: TestIgnored => IgnoredEvent(taskDef.fullyQualifiedName, event.qualifiedName, taskDef.fingerprint, focusMode)
        lock.synchronized:
          handler.handle(sbtEvent)
          sbtEvent.log(loggers, event.duration.toMillis)

    runner.runSuite(td.fullyQualifiedName, Some(eventSubscriber)).map(_ => Array.empty)

trait LoggedEvent(color: String, prefix: String, suiteName: String, testNames: Seq[String]) with
  val fullyQualifiedTestName: String = suiteName + " >> " + testNames.mkString(" >> ")

  def log(loggers: Array[Logger], executionTime: Long): Unit = loggers.foreach(_.info(color + s"[${prefix}] ${fullyQualifiedTestName} (${executionTime} ms)"))

case class SuccessfulEvent(duration: Long, suiteName: String, testNames: Seq[String], fingerprint: Fingerprint, focusMode: Boolean)
extends Event with LoggedEvent(Console.GREEN, "PASSED", suiteName, testNames) with

  override def fullyQualifiedName = suiteName
  override def status = sbt.testing.Status.Success
  override def selector(): sbt.testing.Selector = new NestedTestSelector(suiteName, testNames.mkString(" >> "))
  override def throwable(): sbt.testing.OptionalThrowable = sbt.testing.OptionalThrowable()

case class FailedEvent(duration: Long, suiteName: String, testNames: Seq[String], fingerprint: Fingerprint, focusMode: Boolean, assertionMessage: String, err: Option[Throwable])
extends Event with LoggedEvent(Console.RED, "FAILED", suiteName, testNames) with
  val color = Console.RED // Why are not these inherited form LoggedEvent?
  val prefix = "FAILED"

  override def fullyQualifiedName = suiteName
  override def status = sbt.testing.Status.Failure
  override def selector(): sbt.testing.Selector = new NestedTestSelector(suiteName, testNames.mkString(" >> "))
  override def throwable(): sbt.testing.OptionalThrowable = err

  override def log(loggers: Array[Logger], executionTime: Long): Unit =
    val error = err.map(e => "\n\n" + printErrorWithPrefix(e, s"\t${color}")).getOrElse("")
    loggers.foreach(_.info(color + s"[${prefix}] ${fullyQualifiedTestName} (${executionTime} ms) \n\t${color}${assertionMessage}${error}"))

case class ErrorEvent(duration: Long, suiteName: String, testNames: Seq[String], fingerprint: Fingerprint, focusMode: Boolean, errContext: String, err: Option[Throwable])
extends Event with LoggedEvent(Console.RED, "ERROR", suiteName, testNames) with
  val color = Console.RED // Why are not these inherited form LoggedEvent?
  val prefix = "ERROR"

  override def fullyQualifiedName = suiteName
  override def status = sbt.testing.Status.Failure
  override def selector(): sbt.testing.Selector = new NestedTestSelector(suiteName, testNames.mkString(" >> "))
  override def throwable(): sbt.testing.OptionalThrowable = err
  override def log(loggers: Array[Logger], executionTime: Long): Unit =
    val error = err.map(e => "\n\n" + printErrorWithPrefix(e, s"\t${color}")).getOrElse("")
    loggers.foreach(_.info(color + s"[${prefix}] ${fullyQualifiedTestName} (${executionTime} ms) \n\t${color}${errContext}${error}"))

case class IgnoredEvent(suiteName: String, testNames: Seq[String], fingerprint: Fingerprint, focusMode: Boolean)
extends Event with LoggedEvent(Console.YELLOW, "IGNORED", suiteName, testNames) with

  override def duration = 0L
  override def fullyQualifiedName = suiteName
  override def status = sbt.testing.Status.Ignored
  override def selector(): sbt.testing.Selector = new NestedTestSelector(suiteName, testNames.mkString(" >> "))
  override def throwable(): sbt.testing.OptionalThrowable = sbt.testing.OptionalThrowable()
  override def log(loggers: Array[Logger], executionTime: Long): Unit =
      if (!focusMode) then super.log(loggers, executionTime)

implicit def option2ot(ot: Option[Throwable]): OptionalThrowable =
  ot match
    case Some(e) => sbt.testing.OptionalThrowable(e)
    case None    => sbt.testing.OptionalThrowable()
