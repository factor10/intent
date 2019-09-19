package intent.helpers

import intent.core.{ExpectationResult, Expectation, TestError, TestFailed, TestPassed, TestLanguage, TestSupport}
import scala.concurrent.{Future, ExecutionContext}
import java.util.regex.Pattern

trait Meta:
  self: TestLanguage with TestSupport =>
    def runExpectation(e: => Expectation, expected: String) given (ExecutionContext): Expectation =
      val expectedFileName = getClass.getSimpleName // Assume class X is in X.scala
      new Expectation :
        def evaluate(): Future[ExpectationResult] =
          e.evaluate().flatMap:
            case TestFailed(s) =>
              val expQ = Pattern.quote(expected)
              val fnQ = Pattern.quote(expectedFileName)
              expect(s).toMatch(s"^$expQ.*\\(.*$fnQ".r).evaluate()
            case TestPassed()  => Future.successful(TestFailed("Expected a test failure")) // TODO: Describe the expect call when the macro stuff is in
            case t: TestError  => Future.successful(t)