package intent.core

import scala.concurrent.duration._
import scala.concurrent.{Future, ExecutionContext}
import scala.util.control.NonFatal
import scala.util.{Try, Success, Failure}

import intent.Expect // TODO: dependency on parent package isn't nice

trait IntentStructure:
  private[intent] def allTestCases: Seq[ITestCase]

trait TestLanguage:
  def expect[T](expr: => T): Expect[T] = new Expect[T](expr)

  def whenComplete[T](expr: => Future[T])(impl: T => Expectation): Expectation =
    new Expectation:
      def evaluate(): Future[ExpectationResult] =
        expr.transformWith:
          case Success(r) => impl(r).evaluate()
          case Failure(t) => Future.successful(TestFailed("The Future passed to 'whenComplete' failed"))

  // TODO: Can this be overridden? Or do we need a protected def
  given executionContext as ExecutionContext = ExecutionContext.global

trait IntentStateSyntax[TState] extends IntentStructure with TestLanguage:
  type Transform = TState => TState
  case class SetupPart(name: String, transform: Transform)
  case class TransformAndBlock(transform: Transform, blk: () => Unit)
  case class TestCase(setup: Seq[SetupPart], name: String, impl: TState => Expectation) extends ITestCase:
    def nameParts: Seq[String] = setup.map(_.name) :+ name
    // TODO: Handle setup failure, i.e. that this Future fails
    def run(): Future[TestCaseResult] =
      val before = System.nanoTime
      val state = setup.foldLeft(emptyState)((st, part) => part.transform(st))
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

  def (blockName: String) apply (block: => Unit): Unit =
    blockName - block

// TODO: Remove duplication wrt IntentStateSyntax
// TODO: It would be nice if we could just do 'extends IntentStateSyntax[Future[TState]]',
// TODO: but futureState.flatMap in run below is special here.
trait IntentAsyncStateSyntax[TState] extends IntentStructure with TestLanguage:
  type Transform = Future[TState] => Future[TState]
  type Map = TState => TState
  // TODO: Get rid of Int. Without it, Map and FlatMap have the same type after erasure.
  type FlatMap = (TState, Int) => Future[TState]

  case class SetupPart(name: String, transform: Transform)
  case class TransformAndBlock(transform: Transform, blk: () => Unit)
  case class TestCase(setup: Seq[SetupPart], name: String, impl: TState => Expectation) extends ITestCase:
    def nameParts: Seq[String] = setup.map(_.name) :+ name
    // TODO: Handle setup failure, i.e. that this Future fails
    def run(): Future[TestCaseResult] =
      val before = System.nanoTime
      val futureState = setup.foldLeft(emptyState)((st, part) => part.transform(st))
      futureState.flatMap:
        state =>
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
  def (transform: Map) - (block: => Unit): TransformAndBlock =
    TransformAndBlock(_.map(transform), () => block)

  // hack because "XX via YY - BLK" is evaluated as "XX via (YY - BLK)"
  def (transform: FlatMap) - (block: => Unit): TransformAndBlock =
    TransformAndBlock(_.flatMap(s => transform(s, 0)), () => block)

  def (testName: String) in (testImpl: TState => Expectation): Unit =
    val parts = reverseSetupStack.reverse
    testCases :+= TestCase(parts, testName, testImpl)

  def emptyState: Future[TState]
