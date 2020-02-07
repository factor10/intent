package intent.core.expectations

import intent.core._
import intent.util.DelayedFuture
import scala.util.matching.Regex
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Failure}

class ToCompleteWithExpectation[T](expect: Expect[Future[T]], expected: T)(
  using
    eqq: Eq[T],
    fmt: Formatter[T],
    errFmt: Formatter[Throwable],
    ec: ExecutionContext,
    timeout: TestTimeout
) extends Expectation with

  def evaluate(): Future[ExpectationResult] =
    val timeoutFuture = DelayedFuture(timeout.timeout):
      throw TestTimeoutException()
    Future.firstCompletedOf(Seq(expect.evaluate(), timeoutFuture)).transform:
      case Success(actual) =>
        var comparisonResult = eqq.areEqual(actual, expected)
        if expect.isNegated then comparisonResult = !comparisonResult
        val r =
          if !comparisonResult then
            val actualStr = fmt.format(actual)
            val expectedStr = fmt.format(expected)

            val desc = if expect.isNegated then
              s"Expected Future not to be completed with $expectedStr"
            else
              s"Expected Future to be completed with $expectedStr but found $actualStr"
            expect.fail(desc)
          else
            expect.pass

        Success(r)
      case Failure(t: TestTimeoutException) =>
        Success(expect.fail("Test timed out"))
      case Failure(_) if expect.isNegated =>
        // ok, Future was not completed with <expected>
        Success(expect.pass)
      case Failure(t) =>
        val expectedStr = fmt.format(expected)
        val errorStr = errFmt.format(t)
        val desc = s"Expected Future to be completed with $expectedStr but it failed with $errorStr"
        val r = expect.fail(desc)
        Success(r)
