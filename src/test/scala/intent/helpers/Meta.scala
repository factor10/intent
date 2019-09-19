package intent.helpers

import intent.core.{ExpectationResult, Expectation, TestError, TestFailed, TestPassed, TestLanguage}
import intent.TestSupport
import scala.concurrent.{Future, ExecutionContext}

trait Meta:
  self: TestLanguage with TestSupport =>
    def runExpectation(e: => Expectation, expected: String) given (ExecutionContext): Expectation =
      new Expectation :
        def evaluate(): Future[ExpectationResult] =
          e.evaluate().flatMap:
            case TestFailed(s) => expect(s).toEqual(expected).evaluate()
            case TestPassed()  => Future.successful(TestFailed("Expected a test failure")) // TODO: Describe the expect call when the macro stuff is in
            case t: TestError  => Future.successful(t)