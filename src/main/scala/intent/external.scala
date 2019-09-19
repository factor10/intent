package intent

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
 * Each test suite must be derived from this class in order to be discovered by the
 * test runner.
 */
abstract class TestSuite extends core.TestSuite

trait State[TState] extends core.IntentStateSyntax[TState] with core.TestSupport

trait AsyncState[TState] extends core.IntentAsyncStateSyntax[TState] with core.TestSupport

trait Stateless extends core.IntentStatelessSyntax with core.TestSupport
