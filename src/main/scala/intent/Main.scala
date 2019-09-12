package intent

import java.net.URLClassLoader
import scala.util.control.NonFatal
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
 * Each test suite must be derived from this class, else I could not get the sbt fingerprintint
 * to work.
 * I tried to combine TestSuite with Intent to `TestSuite[T] extends Intent[T]` but that could not
 * be resolved either...
 */
class TestSuite {}

case class TestCaseResult(duration: FiniteDuration, expectationResult: ExpectationResult)

trait ITestCase {
  def nameParts: Seq[String]
  def run(): Future[TestCaseResult]
}

trait Intent[TState] extends FormatterGivens with EqGivens with ExpectGivens {
  type Transform = TState => TState
  case class SetupPart(name: String, transform: Transform)
  case class TransformAndBlock(transform: Transform, blk: () => Unit)
  case class TestCase(setup: Seq[SetupPart], name: String, impl: TState => Expectation) extends ITestCase {
    def nameParts: Seq[String] = setup.map(_.name) :+ name
    def run(): Future[TestCaseResult] = {
      val state = setup.foldLeft(emptyState)((st, part) => part.transform(st))
      val before = System.nanoTime
      try {
        val expectation = impl(state)
        expectation.evaluate().map { result =>
          val elapsed = (System.nanoTime - before).nanos
          TestCaseResult(elapsed, result)
        }
      } catch {
        case NonFatal(t) =>
          val elapsed = (System.nanoTime - before).nanos
          val result = TestError(t)
          Future.successful(TestCaseResult(elapsed, result))
      }
    }
  }

  def allTestCases: Seq[ITestCase] = testCases

  private var testCases: Seq[TestCase] = Seq.empty
  private var reverseSetupStack: Seq[SetupPart] = Seq.empty

  // needed because extension methods are appliced right to left??
  def (blockName: String) via (transformAndBlock: TransformAndBlock): Unit = {
    SetupPart(blockName, transformAndBlock.transform) - transformAndBlock.blk()
  }

  def (setupPart: SetupPart) - (block: => Unit): Unit = {
    reverseSetupStack +:= setupPart
    try block finally reverseSetupStack = reverseSetupStack.tail
  }

  def (blockName: String) - (block: => Unit): Unit = {
    val setupPart = SetupPart(blockName, s => s)
    reverseSetupStack +:= setupPart
    try block finally reverseSetupStack = reverseSetupStack.tail
  }

  // hack because "XX via YY - BLK" is evaluated as "XX via (YY - BLK)"
  def (transform: Transform) - (block: => Unit): TransformAndBlock = {
    TransformAndBlock(transform, () => block)
  }

  def (testName: String) in (testImpl: TState => Expectation): Unit = {
    val parts = reverseSetupStack.reverse
    testCases :+= TestCase(parts, testName, testImpl)
  }

  def expect[T](expr: => T): Expect[T] = new Expect[T](expr)

  def emptyState: TState

  // TODO: Can this be overridden? Or do we need a protected def
  given executionContext as ExecutionContext = ExecutionContext.global
}
