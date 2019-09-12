package intent.core

import scala.concurrent.duration._
import scala.concurrent.{Future, ExecutionContext}
import scala.util.control.NonFatal

import intent.Expect // TODO: dependency on parent package isn't nice

trait IntentStructure:
  private[intent] def allTestCases: Seq[ITestCase]

trait TestLanguage:
  def expect[T](expr: => T): Expect[T] = new Expect[T](expr)

  // TODO: What happens if the Future fails?
  def whenComplete[T](expr: => Future[T])(impl: T => Expectation): Expectation =
    new Expectation:
      def evaluate(): Future[ExpectationResult] = 
        expr.flatMap(impl(_).evaluate())

  // TODO: Can this be overridden? Or do we need a protected def
  given executionContext as ExecutionContext = ExecutionContext.global

trait IntentStateSyntax[TState] extends IntentStructure with TestLanguage:
  type Transform = TState => TState
  case class SetupPart(name: String, transform: Transform)
  case class TransformAndBlock(transform: Transform, blk: () => Unit)
  case class TestCase(setup: Seq[SetupPart], name: String, impl: TState => Expectation) extends ITestCase:
    def nameParts: Seq[String] = setup.map(_.name) :+ name
    def run(): Future[TestCaseResult] =
      val state = setup.foldLeft(emptyState)((st, part) => part.transform(st))
      val before = System.nanoTime
      try
        val expectation = impl(state)
        expectation.evaluate().map { result =>
          val elapsed = (System.nanoTime - before).nanos
          TestCaseResult(elapsed, result)
        }
      catch
        case NonFatal(t) =>
          val elapsed = (System.nanoTime - before).nanos
          val result = TestError(t)
          Future.successful(TestCaseResult(elapsed, result))

  private[intent] override def allTestCases: Seq[ITestCase] = testCases
  private var testCases: Seq[TestCase] = Seq.empty
  private var reverseSetupStack: Seq[SetupPart] = Seq.empty

  // needed because extension methods are appliced right to left??
  def (blockName: String) via (transformAndBlock: TransformAndBlock): Unit =
    SetupPart(blockName, transformAndBlock.transform) - transformAndBlock.blk()

  def (setupPart: SetupPart) - (block: => Unit): Unit =
    reverseSetupStack +:= setupPart
    try block finally reverseSetupStack = reverseSetupStack.tail

  def (blockName: String) - (block: => Unit): Unit =
    val setupPart = SetupPart(blockName, s => s)
    reverseSetupStack +:= setupPart
    try block finally reverseSetupStack = reverseSetupStack.tail

  // hack because "XX via YY - BLK" is evaluated as "XX via (YY - BLK)"
  def (transform: Transform) - (block: => Unit): TransformAndBlock =
    TransformAndBlock(transform, () => block)

  def (testName: String) in (testImpl: TState => Expectation): Unit =
    val parts = reverseSetupStack.reverse
    testCases :+= TestCase(parts, testName, testImpl)

  def emptyState: TState

trait IntentStatelessSyntax extends IntentStructure with TestLanguage:
  case class SetupPart(name: String)
  case class Block(blk: () => Unit)
  case class TestCase(setup: Seq[SetupPart], name: String, impl: () => Expectation) extends ITestCase:
    def nameParts: Seq[String] = setup.map(_.name) :+ name
    def run(): Future[TestCaseResult] =
      val before = System.nanoTime
      try
        val expectation = impl()
        expectation.evaluate().map { result =>
          val elapsed = (System.nanoTime - before).nanos
          TestCaseResult(elapsed, result)
        }
      catch
        case NonFatal(t) =>
          val elapsed = (System.nanoTime - before).nanos
          val result = TestError(t)
          Future.successful(TestCaseResult(elapsed, result))

  private[intent] override def allTestCases: Seq[ITestCase] = testCases
  private var testCases: Seq[TestCase] = Seq.empty
  private var reverseSetupStack: Seq[SetupPart] = Seq.empty

  def (blockName: String) - (block: => Unit): Unit =
    val setupPart = SetupPart(blockName)
    reverseSetupStack +:= setupPart
    try block finally reverseSetupStack = reverseSetupStack.tail

  def (testName: String) in (testImpl: => Expectation): Unit =
    val parts = reverseSetupStack.reverse
    testCases :+= TestCase(parts, testName, () => testImpl)
