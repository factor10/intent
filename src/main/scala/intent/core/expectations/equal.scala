package intent.core.expectations

import intent.core._
import scala.concurrent.Future

class EqualExpectation[T](expect: Expect[T], expected: T)
    given (eqq: Eq[T], fmt: Formatter[T]) extends Expectation:

  def evaluate(): Future[ExpectationResult] =
    val actual = expect.evaluate()

    var comparisonResult = eqq.areEqual(actual, expected)
    if expect.isNegated then comparisonResult = !comparisonResult

    val r = if !comparisonResult then
      val actualStr = fmt.format(actual)
      val expectedStr = fmt.format(expected)

      val desc = if expect.isNegated then
        s"Expected $actualStr not to equal $expectedStr"
      else
        s"Expected $expectedStr but found $actualStr"
      expect.fail(desc)
    else expect.pass
    Future.successful(r)