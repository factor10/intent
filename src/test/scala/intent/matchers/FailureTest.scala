
package intent.matchers

import intent._
import scala.concurrent.Future

class FailureTest extends TestSuite with Intent[Unit]:
  "a toEqual failure" - :
    "is described properly" in { _ =>
      runExpectation(expect(1).toEqual(2), "Expected 2 but found 1")
    }
    "is described properly in the negative" in { _ =>
      runExpectation(expect(1).not.toEqual(1), "Expected 1 to not equal 1")
    }
    "is described properly with Option" in { _ =>
      runExpectation(expect(Some(1)).toEqual(None), "Expected None but found Some(1)")
    }

  def emptyState = ()

  def runExpectation(e: => Expectation, expected: String): Expectation =
    return new Expectation :
      def evaluate(): Future[ExpectationResult] =
        e.evaluate().flatMap:
          case TestFailed(s) => expect(s).toEqual(expected).evaluate()
          case TestPassed()  => Future.successful(TestFailed("Expected a test failure")) // TODO: Describe the expect call when the macro stuff is in 
          case t: TestError  => Future.successful(t)