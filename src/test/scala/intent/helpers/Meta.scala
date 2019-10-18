package intent.helpers

import intent.core._
import scala.concurrent.{Future, ExecutionContext}
import java.util.regex.Pattern

trait Meta
  self: TestLanguage with TestSupport =>
    def runExpectation(e: => Expectation, expected: String)(given ExecutionContext): Expectation =
      val expectedFileName = getClass.getSimpleName // Assume class X is in X.scala
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
