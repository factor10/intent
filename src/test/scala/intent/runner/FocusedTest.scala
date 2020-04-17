package intent.runner

import intent.{TestSuite, State, Stateless, AsyncState}
import intent.core.{Expectation, ExpectationResult, TestError, TestFailed, Subscriber, TestCaseResult, IntentStructure}
import intent.runner.{TestSuiteRunner, TestSuiteError, TestSuiteResult}
import intent.testdata._
import intent.helpers.TestSuiteRunnerTester

import scala.concurrent.{ExecutionContext, Future}

class FocusedTest extends TestSuite with State[FocusedTestCase]:
  "FocusedTest" usingTable (focusedSuites) to:
    "should be focused" in:
      state =>
        expect(state.evaluate().isFocused).toEqual(state.focused)

    "report that correct number of tests were run" in:
      state =>
        whenComplete(state.runAll()):
          possible => possible match
            case Left(_) => fail("Unexpected Left")
            case Right(result) => expect(result.successful).toEqual(state.expectedSuccessful)

    "report that correct number test were ignored" in:
      state =>
        whenComplete(state.runAll()):
          possible => possible match
            case Left(_) => fail("Unexpected Left")
            case Right(result) => expect(result.ignored).toEqual(state.expectedIgnored)

    "no tests should be failed" in:
      state =>
        whenComplete(state.runAll()):
          possible => possible match
            case Left(_) => fail("Unexpected Left")
            case Right(result) => expect(result.failed).toEqual(0)

  def focusedSuites = Seq(
    FocusedTestCase("intent.runner.NestedFocusedStatelessTestSuite", expectedSuccessful = 4, expectedIgnored = 2, focused = true),
    FocusedTestCase("intent.runner.MidBranchFocusedStatelessTestSuite", expectedSuccessful = 3, expectedIgnored = 2, focused = true),
    FocusedTestCase("intent.runner.NestedFocusedStatefulTestSuite", expectedSuccessful = 3, expectedIgnored = 2, focused = true),
    FocusedTestCase("intent.runner.MidBranchFocusedStatefulTestSuite", expectedSuccessful = 3, expectedIgnored = 2, focused = true),
    FocusedTestCase("intent.runner.FocusedStatelessTestSuite", expectedSuccessful = 2, expectedIgnored = 1, focused = true),
    FocusedTestCase("intent.runner.FocusedStatefulTestSuite", expectedSuccessful = 2, expectedIgnored = 1, focused = true),
    FocusedTestCase("intent.runner.FocusedAsyncStatefulTestSuite", expectedSuccessful = 4, expectedIgnored = 1, focused = true),
    FocusedTestCase("intent.runner.FocusedTableDrivenTestSuite", expectedSuccessful = 4, expectedIgnored = 1, focused = true),
    FocusedTestCase("intent.runner.IgnoredStatelessTestSuite", expectedSuccessful = 1, expectedIgnored = 5, focused = false),
    FocusedTestCase("intent.runner.IgnoredTableDrivenTestSuite", expectedSuccessful = 2, expectedIgnored = 3, focused = false),
    FocusedTestCase("intent.runner.IgnoredStatefulTestSuite", expectedSuccessful = 1, expectedIgnored = 3, focused = false),
    FocusedTestCase("intent.runner.IgnoredAsyncStatefulTestSuite", expectedSuccessful = 2, expectedIgnored = 2, focused = false),
  )

case class FocusedTestCase(
  suiteClassName: String = null,
  expectedSuccessful:Int = 0,
  expectedIgnored:Int = 0,
  focused: Boolean = false)(using ec: ExecutionContext) extends TestSuiteRunnerTester

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

class FocusedTableDrivenTestSuite extends State[Unit]:
  "top level" using (()) to:
    "nested level" using (()) to:
      "should not be run" in:
        _ => fail("should not happen")
    "should be run" focus:
      _ => success()

  "another suite" using (()) to:
    "that use a table" usingTable(tableTests) focused:
      "should be run" in:
        _ => success()

  def tableTests = Seq((), (), ())

class FocusedStatelessTestSuite extends Stateless:
  "should not be run" in fail("Test is not expected to run!")
  "should be run" focus success()
  "should also be run" focus success()

class FocusedStatefulTestSuite extends State[Unit]:
  "with state" using (()) to:
    "should not be run" in:
      _ => fail("Test is not expected to run!")
    "should be run" focus:
      _ => success()
    "should also be run" focus:
      _ => success()

class FocusedAsyncStatefulTestSuite extends AsyncState[Unit]:
  "with state" usingAsync (Future.successful(())) to:
    "should not be run" in:
      _ => fail("Test is not expected to run!")
    "should be run" focus:
      _ => success()
    "should also be run" focus:
      _ => success()

  "sibling" usingAsync (Future.successful(())) focused:
    "sibling child" usingAsync (Future.successful(())) to:
      "should be run" in:
         _ => success()
    "should be run" in:
       _ => success()

class IgnoredStatelessTestSuite extends Stateless:
  "top":
    "child" ignored:
      "should not be run" in fail("should not happen")
      "should also not be run" in fail("should not happen")
      "should ignore focus" focus fail("should not happen")
      "focus has less prio" focused:
        "should not be run" in fail("should not happen")
        "should also not be run" in fail("should not happen")
    "should be run" in success()

class IgnoredTableDrivenTestSuite extends State[Unit]:
  "top level" using (()) to:
    "nested level" using (()) to:
      "should be run" in:
        _ => success()

  "another suite" using (()) to:
    "that use a table" usingTable(tableTests) ignored:
      "should *not* be run" in:
        _ => fail("should not happen")
    "should be run" in:
      _ => success()

  def tableTests = Seq((), (), ())

class IgnoredStatefulTestSuite extends State[Unit]:
  "some suite" using (()) ignored:
    "nested suite" using (()) to:
      "should not be run" in:
        _ => fail("should not happen")
    "nested focus has lower prio" using (()) focused:
      "should not be run" in:
          _ => fail("should not happen")

    "should also not be run" in:
        _ => fail("should not happen")

  "another suite" using (()) to:
    "should be run" in:
      _ => success()

class IgnoredAsyncStatefulTestSuite extends AsyncState[Unit]:
  "with state" usingAsync (Future.successful(())) ignored:
    "should not be run" in:
      _ => fail("Test is not expected to run!")
    "should be run" focus:
      _ => fail("Test is not expected to run!")

  "sibling" usingAsync (Future.successful(())) to:
    "sibling child" usingAsync (Future.successful(())) to:
      "should be run" in:
         _ => success()
    "should be run" in:
       _ => success()
