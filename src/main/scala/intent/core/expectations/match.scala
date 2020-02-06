package intent.core.expectations

import intent.core._
import scala.concurrent.Future
import scala.util.matching.Regex

class MatchExpectation[T](expect: Expect[String], re: Regex)(
  using fmt: Formatter[String]
) extends Expectation with
  def evaluate(): Future[ExpectationResult] =
    val actual = expect.evaluate()

    var comparisonResult = re.findFirstIn(actual).isDefined
    if expect.isNegated then comparisonResult = !comparisonResult

    val r = if !comparisonResult then
      val actualStr = fmt.format(actual)
      val expectedStr = re.toString

      val desc = if expect.isNegated then
        s"Expected $actualStr not to match /$expectedStr/"
      else
        s"Expected $actualStr to match /$expectedStr/"
      expect.fail(desc)
    else expect.pass
    Future.successful(r)
