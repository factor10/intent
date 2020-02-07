package intent.core.expectations

import intent.core._
import scala.concurrent.Future
import scala.collection.mutable.ListBuffer
import scala.{Array, Iterable}

private def evalToEqual[T](actual: Iterable[T],
                            expected: Iterable[T],
                            expect: Expect[_],
                            actualListTypeName: String,
                            expectedListTypeName: String)
   (using
      eqq: Eq[T],
      fmt: Formatter[T]
    ): Future[ExpectationResult] =

  def emptyIterator: Iterator[T] = Seq.empty[T].iterator
  def printContents(lb: ListBuffer[String], it: Iterable[T]) =
    if it eq null then "" else lb.mkString("(", ", ", ")")

  val areSameOk = (actual eq expected) && !expect.isNegated
  if areSameOk then
    // Shortcut the logic below. This allows us to test that an infinite list is
    // equal to itself.
    return Future.successful(expect.pass)

  val actualFormatted = ListBuffer[String]()
  val expectedFormatted = ListBuffer[String]()
  val actualIterator = Option(actual).map(_.iterator).getOrElse(emptyIterator)
  val expectedIterator = Option(expected).map(_.iterator).getOrElse(emptyIterator)
  var mismatch = false
  // TODO: Handle very long / infinite collections
  while !mismatch && actualIterator.hasNext && expectedIterator.hasNext do
    val actualNext = actualIterator.next()
    val expectedNext = expectedIterator.next()
    actualFormatted += fmt.format(actualNext)
    expectedFormatted += fmt.format(expectedNext)
    if !eqq.areEqual(actualNext, expectedNext) then
      mismatch = true

  // Must check if one (but not the other) is null, since we use an empty-iterator
  // fallback above (so null gets treated as empty list above).
  val oneIsNull = (actual eq null) ^ (expected eq null)
  val hasDiff = mismatch || actualIterator.hasNext || expectedIterator.hasNext || oneIsNull
  val allGood = if expect.isNegated then hasDiff else !hasDiff

  val r = if !allGood then

    // Collect the rest of the collections, if needed
    while actualIterator.hasNext || expectedIterator.hasNext do
      if actualIterator.hasNext then actualFormatted += fmt.format(actualIterator.next())
      if expectedIterator.hasNext then expectedFormatted += fmt.format(expectedIterator.next())

    val actualStr = actualListTypeName + printContents(actualFormatted, actual)
    val expectedStr = expectedListTypeName + printContents(expectedFormatted, expected)

    val desc = if expect.isNegated then
      s"Expected $actualStr to not equal $expectedStr"
    else
      s"Expected $actualStr to equal $expectedStr"
    expect.fail(desc)
  else expect.pass
  Future.successful(r)

class EqualExpectation[T](expect: Expect[T], expected: T)(
  using eqq: Eq[T], fmt: Formatter[T]
) extends Expectation with

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

class IterableEqualExpectation[T](expect: Expect[Iterable[T]], expected: Iterable[T])(
  using eqq: Eq[T], fmt: Formatter[T]
) extends Expectation with

  def evaluate(): Future[ExpectationResult] =
    val actual = expect.evaluate()
    evalToEqual(actual, expected, expect, listTypeName(actual), listTypeName(expected))


class ArrayEqualExpectation[T](expect: Expect[Array[T]], expected: Iterable[T])(
  using eqq: Eq[T], fmt: Formatter[T]
) extends Expectation with

  def evaluate(): Future[ExpectationResult] =
    val actual = expect.evaluate()
    evalToEqual(actual, expected, expect, "Array", listTypeName(expected))
