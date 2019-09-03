package intent

class AssertionError(msg: String) extends RuntimeException(msg)

class Expect[T](blk: => T) {
  def evaluate(): T = blk
}

trait ExpectGivens {
  def (expect: Expect[T]) toEqual[T] (expected: T) given (eq: Eq[T], fmt: Formatter[T]): Unit = {
    val actual = expect.evaluate()
    if (!eq.areEqual(actual, expected)) {
      val actualStr = fmt.format(actual)
      val expectedStr = fmt.format(expected)
      throw new AssertionError(s"Expected $expectedStr but got $actualStr")
    }
  }
}