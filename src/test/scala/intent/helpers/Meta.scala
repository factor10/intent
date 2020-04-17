package intent.helpers

import intent.core._
import scala.concurrent.{Future, ExecutionContext}
import java.util.regex.Pattern

trait Meta:
  self: TestLanguage with TestSupport =>
    def runExpectation(e: => Expectation, expected: String)(using ExecutionContext): Expectation =
      new Expectation:
        def evaluate(): Future[ExpectationResult] =
          e.evaluate().flatMap:
            case TestFailed(s, _) =>
              val expQ = Pattern.quote(expected)
              val fnQ = Pattern.quote(expectedFileName)
              expect(s).toMatch(s"^$expQ.*\\(.*$fnQ".r).evaluate()
            case TestPassed()   => Future.successful(TestFailed("Expected a test failure", None))
            case TestIgnored()  => Future.successful(TestFailed("Expected a test failure", None))
            case t: TestError   => Future.successful(t)

    def runExpectation(e: => Expectation, exMatcher: PartialFunction[Throwable, Boolean])(using ExecutionContext): Expectation =
      new Expectation:
        def evaluate(): Future[ExpectationResult] =
          e.evaluate().flatMap:
            case TestFailed(_, Some(t)) =>
              val isOk = exMatcher.applyOrElse(t, _ => false)
              val result = if isOk then TestPassed() else TestFailed("Unexpected exception", Some(t))
              Future.successful(result)

            case TestFailed(s, _) => Future.successful(TestFailed(s"Expected a test exception, but got: $s", None))
            case TestPassed()     => Future.successful(TestFailed("Expected a test failure", None))
            case TestIgnored()    => Future.successful(TestFailed("Expected a test failure", None))
            case t: TestError     => Future.successful(t)

    private def expectedFileName = getClass.getSimpleName // Assume class X is in X.scala
