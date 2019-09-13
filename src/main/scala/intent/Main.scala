package intent

import java.net.URLClassLoader
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
 * Each test suite must be derived from this class, else I could not get the sbt fingerprintint
 * to work.
 * I tried to combine TestSuite with Intent to `TestSuite[T] extends Intent[T]` but that could not
 * be resolved either...
 */
abstract class TestSuite extends core.TestSuite

// TODO: Rename this trait, TestSupport is not a good name.
// TODO: Also, maybe it should live in intent.core...
trait TestSupport extends FormatterGivens with EqGivens with ExpectGivens

trait State[TState] extends core.IntentStateSyntax[TState] with TestSupport

trait AsyncState[TState] extends core.IntentAsyncStateSyntax[TState] with TestSupport

trait Stateless extends core.IntentStatelessSyntax with TestSupport
