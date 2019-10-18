package intent.runner

import intent.{TestSuite, State, Stateless, AsyncState}
import intent.core.{Expectation, ExpectationResult, TestError, TestFailed, Subscriber, TestCaseResult, IntentStructure}
import intent.runner.{TestSuiteRunner, TestSuiteError, TestSuiteResult}
import intent.testdata._
import intent.helpers.TestSuiteRunnerTester

import scala.concurrent.{ExecutionContext, Future}

class FocusedTest extends TestSuite with State[FocusedTestCase]:
  // TODO: Convert these tests to table driven tests
  // TOOD: Move the other focus tests to this file

  "FocusedTest" using FocusedTestCase() to :
    "running nested stateless" using (_.nestedStateless) to:
      "should be focused" in:
        state =>
            expect(state.evaluate().isFocused).toEqual(true)

      "report that 2 tests were run" in:
          state =>
            whenComplete(state.runAll()):
              possible => possible match
                case Left(_) => fail("Unexpected Left")
                case Right(result) => expect(result.successful).toEqual(4)

      "report that 2 test were ignored" in:
        state =>
          whenComplete(state.runAll()):
            possible => possible match
              case Left(_) => fail("Unexpected Left")
              case Right(result) => expect(result.ignored).toEqual(2)

      "no tests should be failed" in:
        state =>
          whenComplete(state.runAll()):
            possible => possible match
              case Left(_) => fail("Unexpected Left")
              case Right(result) => expect(result.failed).toEqual(0)

    "running mid-branch stateless" using (_.midBranchStateless) to :
      "should be focused" in:
        state =>
            expect(state.evaluate().isFocused).toEqual(true)

      "report that 2 tests were run" in:
          state =>
            whenComplete(state.runAll()):
              possible => possible match
                case Left(_) => fail("Unexpected Left")
                case Right(result) => expect(result.successful).toEqual(3)

      "report that 2 test were ignored" in:
        state =>
          whenComplete(state.runAll()):
            possible => possible match
              case Left(_) => fail("Unexpected Left")
              case Right(result) => expect(result.ignored).toEqual(2)

      "no tests should be failed" in:
        state =>
          whenComplete(state.runAll()):
            possible => possible match
              case Left(_) => fail("Unexpected Left")
              case Right(result) => expect(result.failed).toEqual(0)

    "running nested stateful" using (_.nestedStateful) to:
      "should be focused" in:
        state =>
            expect(state.evaluate().isFocused).toEqual(true)

      "report that 2 tests were run" in:
          state =>
            whenComplete(state.runAll()):
              possible => possible match
                case Left(_) => fail("Unexpected Left")
                case Right(result) => expect(result.successful).toEqual(3)

      "report that 2 test were ignored" in:
        state =>
          whenComplete(state.runAll()):
            possible => possible match
              case Left(_) => fail("Unexpected Left")
              case Right(result) => expect(result.ignored).toEqual(2)

      "no tests should be failed" in:
        state =>
          whenComplete(state.runAll()):
            possible => possible match
              case Left(_) => fail("Unexpected Left")
              case Right(result) => expect(result.failed).toEqual(0)

    "running mid-branch stateful" using (_.midBranchStateful) to :
      "should be focused" in:
        state =>
          expect(state.evaluate().isFocused).toEqual(true)

      "report that 2 tests were run" in:
        state =>
          whenComplete(state.runAll()):
            possible => possible match
              case Left(_) => fail("Unexpected Left")
              case Right(result) => expect(result.successful).toEqual(3)

      "report that 2 test were ignored" in:
        state =>
          whenComplete(state.runAll()):
            possible => possible match
              case Left(_) => fail("Unexpected Left")
              case Right(result) => expect(result.ignored).toEqual(2)

      "no tests should be failed" in:
        state =>
          whenComplete(state.runAll()):
            possible => possible match
              case Left(_) => fail("Unexpected Left")
              case Right(result) => expect(result.failed).toEqual(0)

case class FocusedTestCase(suiteClassName: String = null) given (ec: ExecutionContext) extends TestSuiteRunnerTester:
  def nestedStateless       = FocusedTestCase("intent.runner.NestedFocusedStatelessTestSuite")
  def midBranchStateless    = FocusedTestCase("intent.runner.MidBranchFocusedStatelessTestSuite")
  def nestedStateful        = FocusedTestCase("intent.runner.NestedFocusedStatefulTestSuite")
  def midBranchStateful     = FocusedTestCase("intent.runner.MidBranchFocusedStatefulTestSuite")

class NestedFocusedStatelessTestSuite extends Stateless:
  "some suite" focused:
    "nested suite":
      "should be run" in success()
      "should also be run" in success()

  "another suite":
    "should *not* be run" in fail("should not happen")

  "a third suite":
    "should include single focus" focus success()

  "should also *not* be run" in fail("should not happen")
  "should be run" focus success()

class MidBranchFocusedStatelessTestSuite extends Stateless:
  "some suite":
    "another suite":
      "should *not* be run A" in fail("should not happen")
      "should also *not* be run B" in fail("should not happen")

    "nested suite" focused:
      "should be run C" in success()
      "should also be run D" in success()

    "a third suite":
      "should include single focus" focus success()

class NestedFocusedStatefulTestSuite extends State[Unit]:
  "focused suite" using (()) focused:
    "nested suite" using (()) to:
      "should be run 1" in:
        _ => success()

      "should also be run 2" in:
         _ => success()

  "another suite" using (()) to:
    "should *not* be run 3" in:
      _ => fail("should not happen")

  "should also *not* be run 4" in:
    _ => fail("should not happen")

  "a third suite" using (()) to:
    "should include single focus" focus:
      _ => success()

class MidBranchFocusedStatefulTestSuite extends State[Unit]:
  "some suite" using (()) to:
    "another suite" using (()) to:
      "should *not* be run" in:
        _ => fail("should not happen")

      "should also *not* be run 4" in:
        _ => fail("should not happen")

    "nested suite" using (()) focused:
      "should be run" in:
        _ => success()

      "should also be run" in:
        _ => success()

    "a third suite" using (()) to:
      "should include single focus" focus:
        _ => success()