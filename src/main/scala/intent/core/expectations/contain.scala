package intent.core.expectations

import intent.core._
import scala.concurrent.Future
import scala.collection.mutable.ListBuffer
import scala.Array

private def evalToContain[T](actual: IterableOnce[T],
                              expected: T,
                              expect: Expect[_],
                              listTypeName: String)
   (given
      eqq: Eq[T],
      fmt: Formatter[T],
      cutoff: ListCutoff
    ): Future[ExpectationResult] =
  val seen = ListBuffer[String]()
  var found = false
  val iterator = actual.iterator
  val shouldNotFind = expect.isNegated
  var breakEarly = false
  var itemsChecked = 0
  while !breakEarly && iterator.hasNext do
    if itemsChecked >= cutoff.maxItems then
      breakEarly = true
      // This results in failure output like: List(X, ...)
      seen.takeInPlace(cutoff.printItems)
    else
      val next = iterator.next()
      seen += fmt.format(next)
      if !found && eqq.areEqual(next, expected) then
        found = true
        if shouldNotFind then
          breakEarly = true
      itemsChecked += 1

  val allGood = if expect.isNegated then !found else found

  val r = if !allGood then
    if iterator.hasNext then
      seen += "..."
    val actualStr = listTypeName + seen.mkString("(", ", ", ")")
    val expectedStr = fmt.format(expected)

    val desc = if expect.isNegated then
      s"Expected $actualStr not to contain $expectedStr"
    else
      s"Expected $actualStr to contain $expectedStr"
    expect.fail(desc)
  else expect.pass
  Future.successful(r)

class IterableContainExpectation[T](expect: Expect[IterableOnce[T]], expected: T)
 (given
    eqq: Eq[T],
    fmt: Formatter[T],
    cutoff: ListCutoff
  ) extends Expectation

  def evaluate(): Future[ExpectationResult] =
    val actual = expect.evaluate()
    evalToContain(actual, expected, expect, listTypeName(actual))

class ArrayContainExpectation[T](expect: Expect[Array[T]], expected: T)
 (given
    eqq: Eq[T],
    fmt: Formatter[T],
    cutoff: ListCutoff
  ) extends Expectation

  def evaluate(): Future[ExpectationResult] =
    val actual = expect.evaluate()
    evalToContain(actual, expected, expect, "Array")
