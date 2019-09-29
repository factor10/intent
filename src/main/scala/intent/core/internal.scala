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

/**
  * Base structure for Intent test cases, whether they are async, sync, stateful or stateless.
  * The structure is based on asynchronous execution, since it's possible to fit sync in async,
  * but not vice versa.
  */
trait IntentStateBase[TState] extends IntentStructure with TestLanguage:
  type Map = TState => TState
  type FlatMap = TState => Future[TState]

  protected[intent] def isStateful: Boolean

  private[intent] sealed trait Context:
    def name: String
    def transform(f: Future[Option[TState]]): Future[Option[TState]]
    def position: Position
  private[intent] sealed case class ContextInit(name: String, init: () => Future[TState], position: Position) extends Context:
    def transform(f: Future[Option[TState]]) = init().map(Some.apply)
  private[intent] sealed case class ContextMap(name: String, tx: Map, position: Position) extends Context:
    def transform(f: Future[Option[TState]]) = f.map(_.map(tx))
  private[intent] sealed case class ContextFlatMap(name: String, tx: FlatMap, position: Position) extends Context:
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

      if isStateful && contexts.size == 0 then
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
        case Right(opt) =>
          try
            // The getOrElse+asInstanceOf part is ugly, but it is only required in the
            // stateless case, in which case TState == Unit.
            val expectation = impl(opt.getOrElse(()).asInstanceOf[TState])
            expectation.evaluate().map { result =>
              val elapsed = (System.nanoTime - before).nanos
              TestCaseResult(elapsed, nameParts, result)
            }
          catch
            case NonFatal(t) =>
              Future.successful(failure("Test error", Some(t), tcPosition))

  private[intent] override def allTestCases: Seq[ITestCase] = testCases
  private[intent] override def isFocused: Boolean = inFocusedMode

  private[intent] def withContext(ctx: Context)(block: => Unit): Unit =
    reverseContextStack +:= ctx
    try block finally reverseContextStack = reverseContextStack.tail

  private[intent] def contextsInOrder: Seq[Context] = reverseContextStack.reverse
  private[intent] def addTestCase(tc: ITestCase): Unit = testCases :+= tc
  private[intent] def rewriteTestCases(mapper: ITestCase => ITestCase): Unit =
    testCases = testCases.map(mapper)

  private var testCases: Seq[ITestCase] = Seq.empty
  private var reverseContextStack: Seq[Context] = Seq.empty
  protected var inFocusedMode: Boolean = false

  private[intent] def enableFocusedMode(): Unit =
    // If this is the first focused test, any existing testCase was not focused,
    // and hence should be converted to ignored to ignored tests (without execution)
    if !inFocusedMode then
      inFocusedMode = true
      rewriteTestCases(existing => IgnoredTestCase(existing.nameParts))

/**
  * Provides the Intent stateful test syntax, i.e. where contexts arrange state and
  * test cases act and assert on the state. 
  */
trait IntentStateSyntax[TState] extends IntentStateBase[TState]:
  protected[intent] override def isStateful = true

  def (context: String) using (init: => TState) given (pos: Position) : Context =
    ContextInit(context, () => Future.successful(init), pos)
  def (context: String) using (tx: Map) given (pos: Position) : Context =
    ContextMap(context, tx, pos)

  def (ctx: Context) to (block: => Unit): Unit = withContext(ctx)(block)

  def (testName: String) in (testImpl: TState => Expectation) given (pos: Position): Unit =
    // When in focused mode, all "ordinary" tests becomes ignored
    val contexts = contextsInOrder
    if inFocusedMode then
      addTestCase(IgnoredTestCase(contexts.map(_.name) :+ testName))
    else
      addTestCase(TestCase(contexts, testName, testImpl, pos))

  def (testName: String) ignore (testImpl: TState => Expectation): Unit =
    val contexts = contextsInOrder
    addTestCase(IgnoredTestCase(contexts.map(_.name) :+ testName))

  def (testName: String) focus (testImpl: TState => Expectation) given (pos: Position): Unit =
    enableFocusedMode()
    val contexts = contextsInOrder
    addTestCase(TestCase(contexts, testName, testImpl, pos))

/**
  * Provides the Intent stateless test syntax, i.e. where contexts are merely structural
  * and test cases are entirely responsible for arrange-act-assert.
  */
trait IntentStatelessSyntax extends IntentStateBase[Unit]:

  protected[intent] override def isStateful = false

  def (testName: String) in (testImpl: => Expectation) given (pos: Position): Unit =
    // When in focused mode, all "ordinary" tests becomes ignored
    val contexts = contextsInOrder
    if inFocusedMode then
      addTestCase(IgnoredTestCase(contexts.map(_.name) :+ testName))
    else
      addTestCase(TestCase(contexts, testName, _ => testImpl, pos))

  def (blockName: String) apply (block: => Unit) given (pos: Position): Unit =
    val ctx = ContextInit(blockName, () => Future.successful(()), pos)
    withContext(ctx)(block)

  def (testName: String) ignore (testImpl: => Expectation): Unit =
    val contexts = contextsInOrder
    addTestCase(IgnoredTestCase(contexts.map(_.name) :+ testName))

  def (testName: String) focus (testImpl: => Expectation) given (pos: Position): Unit =
    enableFocusedMode()
    val contexts = contextsInOrder
    addTestCase(TestCase(contexts, testName, _ => testImpl, pos))

trait IntentAsyncStateSyntax[TState] extends IntentStateBase[TState]:

  protected[intent] override def isStateful = true

  def (context: String) using (init: => TState) given (pos: Position): Context = ContextInit(context, () => Future.successful(init), pos)
  def (context: String) usingAsync (init: => Future[TState]) given (pos: Position): Context = ContextInit(context, () => init, pos)
  def (context: String) using (tx: Map) given (pos: Position): Context = ContextMap(context, tx, pos)
  def (context: String) usingAsync (fmc: FlatMap) given (pos: Position): Context = ContextFlatMap(context, fmc, pos)

  def (ctx: Context) to (block: => Unit): Unit =
    withContext(ctx)(block)

  def (testName: String) in (testImpl: TState => Expectation) given (pos: Position): Unit =
    // When in focused mode, all "ordinary" tests becomes ignored
    val contexts = contextsInOrder
    if inFocusedMode then
      addTestCase(IgnoredTestCase(contexts.map(_.name) :+ testName))
    else
      addTestCase(TestCase(contexts, testName, testImpl, pos))

  def (testName: String) ignore (testImpl: TState => Expectation): Unit =
      val contexts = contextsInOrder
      addTestCase(IgnoredTestCase(contexts.map(_.name) :+ testName))

  def (testName: String) focus (testImpl: TState => Expectation) given (pos: Position): Unit =
    enableFocusedMode()
    val contexts = contextsInOrder
    addTestCase(TestCase(contexts, testName, testImpl, pos))
