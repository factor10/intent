package intent.core

import scala.concurrent.duration._
import scala.concurrent.{Future, ExecutionContext}
import scala.util.control.NonFatal
import scala.util.{Try, Success, Failure}
import scala.reflect.ClassTag

import intent.macros.Position
import intent.core.PositionDescription

private class ShouldNotHappenException(msg: String) extends RuntimeException(msg)

trait TestSupport extends FormatterGivens with EqGivens with ExpectGivens

trait IntentStructure:
  private[intent] def allTestCases: Seq[ITestCase]

  /**
   * A focused intent contains at least one focused test.
   */
  private[intent] def isFocused: Boolean

case class IgnoredTestCase(nameParts: Seq[String]) extends ITestCase:
  def run(): Future[TestCaseResult] =
    Future.successful(TestCaseResult(Duration.Zero, nameParts, TestIgnored()))

trait TestLanguage:
  def expect[T](expr: => T) given (pos: Position): Expect[T] = new Expect[T](expr, pos)

  def whenComplete[T](expr: => Future[T])(impl: T => Expectation) given (pos: Position): Expectation =
    import PositionDescription._
    new Expectation:
      def evaluate(): Future[ExpectationResult] =
        expr.transformWith:
          case Success(r) => impl(r).evaluate()
          case Failure(t) => Future.successful(
            TestFailed(pos.contextualize("The Future passed to 'whenComplete' failed"), Some(t)))

  def fail(reason: String) given (pos: Position) =
    import PositionDescription._
    new Expectation:
      def evaluate() = Future.successful(TestFailed(pos.contextualize(reason), None))

  def success() given (pos: Position) =
    import PositionDescription._
    new Expectation:
      def evaluate() = Future.successful(TestPassed())

  // TODO: Can this be overridden? Or do we need a protected def
  given executionContext as ExecutionContext = ExecutionContext.global

trait IntentStateSyntax[TState] extends IntentStructure with TestLanguage:
  type Transform = TState => TState

  trait Context:
    def name: String
    def transform(opt: Option[TState]): Option[TState]
    def position: Position
  case class ContextInit(name: String, init: () => TState, position: Position) extends Context:
    def transform(opt: Option[TState]) = Some(init())
  case class ContextTx(name: String, tx: Transform, position: Position) extends Context:
    def transform(opt: Option[TState]) = opt.map(tx)

  case class TestCase(contexts: Seq[Context], name: String, impl: TState => Expectation, tcPosition: Position) extends ITestCase:
    def nameParts: Seq[String] = contexts.map(_.name) :+ name
    def run(): Future[TestCaseResult] =
      import PositionDescription._

      val before = System.nanoTime
      def result(msg: String, ex: Option[Throwable], pos: Position, er: (String, Option[Throwable]) => ExpectationResult): TestCaseResult =
        val elapsed = (System.nanoTime - before).nanos
        val result = er(pos.contextualize(msg), ex)
        TestCaseResult(elapsed, nameParts, result)

      def error(msg: String, ex: Option[Throwable], pos: Position): TestCaseResult =
        result(msg, ex, pos, TestError.apply)

      def failure(msg: String, ex: Option[Throwable], pos: Position): TestCaseResult =
        result(msg, ex, pos, TestFailed.apply)

      if contexts.size == 0 then
        return Future.successful(error("Top-level test cases are not allowed in a state-based test suite", None, tcPosition))

      // Execute the contexts. If a transformation results in None, something is seriously wrong
      // so treat as error. If an exception is thrown, treat as test failure (since we have started
      // to execute the test - state setup is part of the test).
      val postSetup = contexts.foldLeft[Either[TestCaseResult, Option[TState]]](Right(None))((acc, ctx) => acc.flatMap {
        stateOpt =>
          try
            ctx.transform(stateOpt) match
              case s@Some(_) => Right(s)
              case None =>
                Left(error(s"""Unexpected: state folding for context \"${ctx.name}\" didn't produce a state""", None, ctx.position))
          catch
            case NonFatal(t) =>
              Left(failure(s"""The state setup for context \"${ctx.name}\" failed""", Some(t), ctx.position))
      })

      postSetup match
        case Left(r) => Future.successful(r)
        case Right(Some(state)) =>
          try
            val expectation = impl(state)
            expectation.evaluate().map { result =>
              val elapsed = (System.nanoTime - before).nanos
              TestCaseResult(elapsed, nameParts, result)
            }
          catch
            case NonFatal(t) =>
              Future.successful(failure("Test failure", Some(t), tcPosition))

        case _ => ??? // should not happen since we handle None above

  private[intent] override def allTestCases: Seq[ITestCase] = testCases
  private[intent] override def isFocused: Boolean = inFocusedMode

  private var testCases: Seq[ITestCase] = Seq.empty
  private var reverseContextStack: Seq[Context] = Seq.empty
  private var inFocusedMode: Boolean = false

  def (context: String) using (init: => TState) given (pos: Position) : Context = ContextInit(context, () => init, pos)
  def (context: String) using (tx: Transform) given (pos: Position) : Context = ContextTx(context, tx, pos)

  def (ctx: Context) to (block: => Unit): Unit =
    reverseContextStack +:= ctx
    try block finally reverseContextStack = reverseContextStack.tail

  def (testName: String) in (testImpl: TState => Expectation) given (pos: Position): Unit =
    // When in focused mode, all "ordinary" tests becomes ignored
    if (inFocusedMode) then
      val contexts = reverseContextStack.reverse
      testCases :+= IgnoredTestCase(contexts.map(_.name) :+ testName)
    else
      val contexts = reverseContextStack.reverse
      testCases :+= TestCase(contexts, testName, testImpl, pos)

  def (testName: String) ignore (testImpl: TState => Expectation): Unit =
    val contexts = reverseContextStack.reverse
    testCases :+= IgnoredTestCase(contexts.map(_.name) :+ testName)

  def (testName: String) focus (testImpl: TState => Expectation) given (pos: Position): Unit =
    // If this is the first focused test, any existing testCase was not focused,
    // and hence should be converted to ignored to ignored tests (without execution)
    if !inFocusedMode then
      inFocusedMode = true
      testCases = testCases.map(existing => IgnoredTestCase(existing.nameParts))

    val contexts = reverseContextStack.reverse
    testCases :+= TestCase(contexts, testName, testImpl, pos)

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
          TestCaseResult(elapsed, nameParts, result)
        }
      catch
        case NonFatal(t) =>
          val elapsed = (System.nanoTime - before).nanos
          val result = TestFailed("Test failure", Some(t))
          Future.successful(TestCaseResult(elapsed, nameParts, result))

  private[intent] override def allTestCases: Seq[ITestCase] = testCases
  private[intent] override def isFocused: Boolean = inFocusedMode

  private var testCases: Seq[ITestCase] = Seq.empty
  private var reverseSetupStack: Seq[SetupPart] = Seq.empty
  private var inFocusedMode = false

  def (testName: String) in (testImpl: => Expectation): Unit =
    // When in focused mode, all "ordinary" tests becomes ignored
    if inFocusedMode then
      val parts = reverseSetupStack.reverse
      testCases :+= IgnoredTestCase(parts.map(_.name) :+ testName)
    else
      val parts = reverseSetupStack.reverse
      testCases :+= TestCase(parts, testName, () => testImpl)

  def (blockName: String) apply (block: => Unit): Unit =
    val setupPart = SetupPart(blockName)
    reverseSetupStack +:= setupPart
    try block finally reverseSetupStack = reverseSetupStack.tail

  def (testName: String) ignore (testImpl: => Expectation): Unit =
    val parts = reverseSetupStack.reverse
    testCases :+= IgnoredTestCase(parts.map(_.name) :+ testName)

  def (testName: String) focus (testImpl: => Expectation): Unit =
    // If this is the first focused test, any existing testCase was not focused,
    // and hence should be converted to ignored to ignored tests (without execution)
    if !inFocusedMode then
      inFocusedMode = true
      testCases = testCases.map(existing => IgnoredTestCase(existing.nameParts))

    val parts = reverseSetupStack.reverse
    testCases :+= TestCase(parts, testName, () => testImpl)

// TODO: Remove duplication wrt IntentStateSyntax
// TODO: It would be nice if we could just do 'extends IntentStateSyntax[Future[TState]]',
// TODO: but futureState.flatMap in run below is special here.
trait IntentAsyncStateSyntax[TState] extends IntentStructure with TestLanguage:
  type Map = TState => TState
  type FlatMap = TState => Future[TState]

  trait Context:
    def name: String
    def transform(f: Future[Option[TState]]): Future[Option[TState]]
    def position: Position
  case class ContextInit(name: String, init: () => Future[TState], position: Position) extends Context:
    def transform(f: Future[Option[TState]]) = init().map(Some.apply)
  case class ContextMap(name: String, tx: Map, position: Position) extends Context:
    def transform(f: Future[Option[TState]]) = f.map(_.map(tx))
  case class ContextFlatMap(name: String, tx: FlatMap, position: Position) extends Context:
    def transform(f: Future[Option[TState]]) =
      f.flatMap:
        case Some(state) => tx(state).map(Some.apply)
        case None        => throw ShouldNotHappenException("Unexpected state None after Future transform")

  case class TestCase(contexts: Seq[Context], name: String, impl: TState => Expectation, tcPosition: Position) extends ITestCase:
    def nameParts: Seq[String] = contexts.map(_.name) :+ name
    def run(): Future[TestCaseResult] =
      import PositionDescription._

      val before = System.nanoTime
      def result(msg: String, ex: Option[Throwable], pos: Position, er: (String, Option[Throwable]) => ExpectationResult): TestCaseResult =
        val elapsed = (System.nanoTime - before).nanos
        val result = er(pos.contextualize(msg), ex)
        TestCaseResult(elapsed, nameParts, result)

      def error(msg: String, ex: Option[Throwable], pos: Position): TestCaseResult =
        result(msg, ex, pos, TestError.apply)

      def failure(msg: String, ex: Option[Throwable], pos: Position): TestCaseResult =
        result(msg, ex, pos, TestFailed.apply)

      if contexts.size == 0 then
        return Future.successful(error("Top-level test cases are not allowed in a state-based test suite", None, tcPosition))

      val postSetup = contexts.foldLeft(Future.successful[Either[TestCaseResult, Option[TState]]](Right(None)))((fut, ctx) => fut.flatMap {
        case l@Left(_) => Future.successful(l)
        case Right(stateOpt) =>
          try
            ctx.transform(Future.successful(stateOpt)).transform :
              case Success(newStateOpt) => Success(Right(newStateOpt))
              case Failure(t: ShouldNotHappenException) =>
                Success(Left(error(s"""${t.getMessage} for context \"${ctx.name}\"""", None, ctx.position)))
              case Failure(t) =>
                Success(Left(failure(s"""The state setup for context \"${ctx.name}\" failed""", Some(t), ctx.position)))
          catch
            case NonFatal(t) =>
              // Should not happen since we control our Context classes
              Future.successful(Left(error(s"""The transformation for context \"${ctx.name}\" failed""", Some(t), ctx.position)))
      })

      postSetup.flatMap :
        case Left(r) => Future.successful(r)
        case Right(Some(state)) =>
          try
            val expectation = impl(state)
            expectation.evaluate().map { result =>
              val elapsed = (System.nanoTime - before).nanos
              TestCaseResult(elapsed, nameParts, result)
            }
          catch
            case NonFatal(t) =>
              Future.successful(failure("Test error", Some(t), tcPosition))

        case _ => ??? // should not happen since we handle None above

  private[intent] override def allTestCases: Seq[ITestCase] = testCases
  private[intent] override def isFocused: Boolean = inFocusedMode

  private var testCases: Seq[ITestCase] = Seq.empty
  private var reverseContextStack: Seq[Context] = Seq.empty
  private var inFocusedMode: Boolean = false

  def (context: String) using (init: => TState) given (pos: Position): Context = ContextInit(context, () => Future.successful(init), pos)
  def (context: String) usingAsync (init: => Future[TState]) given (pos: Position): Context = ContextInit(context, () => init, pos)
  def (context: String) using (tx: Map) given (pos: Position): Context = ContextMap(context, tx, pos)
  def (context: String) usingAsync (fmc: FlatMap) given (pos: Position): Context = ContextFlatMap(context, fmc, pos)

  def (ctx: Context) to (block: => Unit): Unit =
    reverseContextStack +:= ctx
    try block finally reverseContextStack = reverseContextStack.tail

  def (testName: String) in (testImpl: TState => Expectation) given (pos: Position): Unit =
    // When in focused mode, all "ordinary" tests becomes ignored
    if inFocusedMode then
      val contexts = reverseContextStack.reverse
      testCases :+= IgnoredTestCase(contexts.map(_.name) :+ testName)
    else
      val contexts = reverseContextStack.reverse
      testCases :+= TestCase(contexts, testName, testImpl, pos)

  def (testName: String) ignore (testImpl: TState => Expectation): Unit =
      val contexts = reverseContextStack.reverse
      testCases :+= IgnoredTestCase(contexts.map(_.name) :+ testName)

  def (testName: String) focus (testImpl: TState => Expectation) given (pos: Position): Unit =
    // If this is the first focused test, any existing testCase was not focused,
    // and hence should be converted to ignored to ignored tests (without execution)
    if !inFocusedMode then
      inFocusedMode = true
      testCases = testCases.map(existing => IgnoredTestCase(existing.nameParts))

    val contexts = reverseContextStack.reverse
    testCases :+= TestCase(contexts, testName, testImpl, pos)
