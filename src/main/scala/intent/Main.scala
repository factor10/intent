package intent

import java.net.URLClassLoader
import scala.util.control.NonFatal
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.Future

/**
 * Seems to be required in order to use the SBT test-class fingerprinting.
 * Could not get trait alone to work...
 */
class IntentMaker {}

case class TestCaseResult(duration: FiniteDuration, expectationResult: ExpectationResult)

trait ITestCase {
  def nameParts: Seq[String]
  def run(): Future[TestCaseResult]
}

trait Intent[TState] extends FormatterGivens with EqGivens with ExpectGivens {
  type Transform = TState => TState
  case class SetupPart(name: String, transform: Transform)
  case class TransformAndBlock(transform: Transform, blk: () => Unit)
  case class TestCase(setup: Seq[SetupPart], name: String, impl: TState => Unit) extends ITestCase {
    def nameParts: Seq[String] = setup.map(_.name)
    def run(): Future[TestCaseResult] = {
      val state = setup.foldLeft(emptyState)((st, part) => part.transform(st))
      impl(state)
      ???
    }
  }

  def allTestCases: Seq[ITestCase] = testCases

  private var testCases: Seq[TestCase] = Seq.empty
  private var reverseSetupStack: Seq[SetupPart] = Seq.empty

  // behövs för att extension-metoder appliceras höger till vänster??
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

  // hack för att "XX via YY - BLK" evalueras som "XX via (YY - BLK)"
  def (transform: Transform) - (block: => Unit): TransformAndBlock = {
    TransformAndBlock(transform, () => block)
  }

  def (testName: String) in (testImpl: TState => Unit): Unit = {
    val parts = reverseSetupStack.reverse
    testCases :+= TestCase(parts, testName, testImpl)
  }

  def expect[T](expr: => T): Expect[T] = new Expect[T](expr)

  def emptyState: TState
}
