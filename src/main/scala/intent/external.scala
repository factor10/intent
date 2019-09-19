package intent

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
 * Each test suite must be derived from this class in order to be discovered by the
 * test runner.
 */
abstract class TestSuite extends core.TestSuite

// TODO: Rename this trait, TestSupport is not a good name.
// TODO: Also, maybe it should live in intent.core...
trait TestSupport extends core.FormatterGivens with core.EqGivens with core.ExpectGivens

trait State[TState] extends core.IntentStateSyntax[TState] with TestSupport

trait AsyncState[TState] extends core.IntentAsyncStateSyntax[TState] with TestSupport

trait Stateless extends core.IntentStatelessSyntax with TestSupport
