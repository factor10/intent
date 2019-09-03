package intent

class AssertionError(msg: String) extends RuntimeException(msg)

class Expect[T](blk: => T) {
  def toEqual(other: T) given (eq: Eq[T], fmt: Formatter[T]): Unit = {
    val value = blk
    if (!eq.areEqual(value, other)) {
      val actualStr = fmt.format(value)
      val expectedStr = fmt.format(other)
      throw new AssertionError(s"Expected $expectedStr but got $actualStr")
    }
  }
}
