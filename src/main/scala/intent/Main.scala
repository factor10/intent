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
class TestSuite extends core.TestSuite

// TODO: Rename this trait, TestSupport is not a good name.
// TODO: Also, maybe it should live in intent.core...
trait TestSupport extends FormatterGivens with EqGivens with ExpectGivens

trait Intent[TState] extends core.IntentStateSyntax[TState] with TestSupport

// TODO: Rename, this should perhaps be the "default" trait
trait IntentStateless extends core.IntentStatelessSyntax with TestSupport
