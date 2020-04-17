package intent.core.expectations

import intent.core._
import scala.concurrent.Future

class LengthExpectation[T](expect: Expect[IterableOnce[T]], expected: Int) extends Expectation:
  def evaluate(): Future[ExpectationResult] =
    val actual = expect.evaluate()
    val actualLength = actual.iterator.size
    var r = if expect.isNegated && actualLength == expected then       expect.fail(s"Expected size *not* to be $expected but was $actualLength")
            else if expect.isNegated && actualLength != expected then  expect.pass
            else if actualLength != expected then                      expect.fail(s"Expected size to be $expected but was $actualLength")
            else                                                       expect.pass

    Future.successful(r)
