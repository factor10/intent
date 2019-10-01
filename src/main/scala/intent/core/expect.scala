package intent.core

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Failure}
import scala.collection.IterableOnce
import scala.collection.mutable.ListBuffer
import scala.language.implicitConversions
import scala.util.matching.Regex
import scala.reflect.ClassTag

import intent.core.{Expectation, ExpectationResult, TestPassed, TestFailed, PositionDescription}
import intent.macros.Position

class CompoundExpectation(inner: Seq[Expectation]) given (ec: ExecutionContext) extends Expectation:
  def evaluate(): Future[ExpectationResult] =
    val innerFutures = inner.map(_.evaluate())
    Future.sequence(innerFutures).map { results =>
      // any failure => failure
      ???
    }

class Expect[T](blk: => T, position: Position, negated: Boolean = false):
  import PositionDescription._

  def evaluate(): T = blk
  def isNegated: Boolean = negated
  def negate(): Expect[T] = new Expect(blk, position, !negated)

  def fail(desc: String): ExpectationResult = TestFailed(position.contextualize(desc), None)
  def pass: ExpectationResult               = TestPassed()

trait ExpectGivens {

  def (expect: Expect[T]) not[T]: Expect[T] = expect.negate()

  def (expect: Expect[T]) toEqual[T] (expected: T) given (eqq: Eq[T], fmt: Formatter[T]): Expectation =
    new Expectation:
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

  // toMatch is partial
  def (expect: Expect[String]) toMatch[T] (re: Regex) given (fmt: Formatter[String]): Expectation =
    new Expectation:
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

  def (expect: Expect[String]) toMatch[T] (re: String) given (fmt: Formatter[String]): Expectation =
    expect.toMatch(re.r)

  def (expect: Expect[Future[T]]) toCompleteWith[T] (expected: T) 
      given (
        eqq: Eq[T], 
        fmt: Formatter[T], 
        errFmt: Formatter[Throwable], 
        ec: ExecutionContext
      ): Expectation =
    new Expectation:
      def evaluate(): Future[ExpectationResult] =
        expect.evaluate().transform:
          case Success(actual) =>
            var comparisonResult = eqq.areEqual(actual, expected)
            if expect.isNegated then comparisonResult = !comparisonResult

            val r = if !comparisonResult then {
              val actualStr = fmt.format(actual)
              val expectedStr = fmt.format(expected)

              val desc = if expect.isNegated then
                s"Expected Future not to be completed with $expectedStr"
              else
                s"Expected Future to be completed with $expectedStr but found $actualStr"
              expect.fail(desc)
            } else expect.pass
            Success(r)
          case Failure(_) if expect.isNegated =>
            // ok, Future was not completed with <expected>
            Success(expect.pass)
          case Failure(t) =>
            val expectedStr = fmt.format(expected)
            val errorStr = errFmt.format(t)
            val desc = s"Expected Future to be completed with $expectedStr but it failed with $errorStr"
            val r = expect.fail(desc)
            Success(r)

  private def listTypeName[T](actual: IterableOnce[T]): String =
    actual.getClass match
      case c if classOf[List[_]].isAssignableFrom(c) => "List"
      case c if classOf[scala.collection.mutable.ArraySeq[_]].isAssignableFrom(c)  => "Array"
      case c => c.getSimpleName

  private def evalToContain[T](actual: IterableOnce[T],
                               expected: T,
                               expect: Expect[_],
                               listTypeName: String)
      given (
        eqq: Eq[T],
        fmt: Formatter[T]
      ): Future[ExpectationResult] =
    val seen = ListBuffer[String]()
    var found = false
    val iterator = actual.iterator
    val shouldNotFind = expect.isNegated
    var breakEarly = false
    while !breakEarly && iterator.hasNext do
      val next = iterator.next()
      seen += fmt.format(next)
      if !found && eqq.areEqual(next, expected) then
        found = true
        if shouldNotFind then
          breakEarly = true
      // TODO: use some heuristic here. Should we continue to collect item string representations? For how long?

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

  // We use ClassTag here to avoid "double definition error" wrt Expect[IterableOnce[T]]
  def (expect: Expect[Array[T]]) toContain[T : ClassTag] (expected: T) 
      given (
        eqq: Eq[T],
        fmt: Formatter[T]
      ): Expectation =
    new Expectation:
      def evaluate(): Future[ExpectationResult] =
        val actual = expect.evaluate()
        evalToContain(actual, expected, expect, "Array")

  def (expect: Expect[IterableOnce[T]]) toContain[T] (expected: T) 
      given (
        eqq: Eq[T],
        fmt: Formatter[T]
      ): Expectation =
    new Expectation:
      def evaluate(): Future[ExpectationResult] =
        val actual = expect.evaluate()
        evalToContain(actual, expected, expect, listTypeName(actual))

  private def evalToEqual[T](actual: Iterable[T],
                             expected: Iterable[T],
                             expect: Expect[_],
                             actualListTypeName: String,
                             expectedListTypeName: String)
      given (
        eqq: Eq[T],
        fmt: Formatter[T]
      ): Future[ExpectationResult] =

    val areSameOk = (actual eq expected) && !expect.isNegated
    if areSameOk then
      // Shortcut the logic below. This allows us to test that an infinite list is
      // equal to itself.
      return Future.successful(expect.pass)

    val actualFormatted = ListBuffer[String]()
    val expectedFormatted = ListBuffer[String]()
    val actualIterator = actual.iterator
    val expectedIterator = expected.iterator
    var mismatch = false
    // TODO: Handle very long / infinite collections
    while !mismatch && actualIterator.hasNext && expectedIterator.hasNext do
      val actualNext = actualIterator.next()
      val expectedNext = expectedIterator.next()
      actualFormatted += fmt.format(actualNext)
      expectedFormatted += fmt.format(expectedNext)
      if !eqq.areEqual(actualNext, expectedNext) then
        mismatch = true
    
    val hasDiff = mismatch || actualIterator.hasNext || expectedIterator.hasNext
    val allGood = if expect.isNegated then hasDiff else !hasDiff

    val r = if !allGood then

      // Collect the rest of the collections, if needed
      while actualIterator.hasNext || expectedIterator.hasNext do
        if actualIterator.hasNext then actualFormatted += fmt.format(actualIterator.next())
        if expectedIterator.hasNext then expectedFormatted += fmt.format(expectedIterator.next())

      val actualStr = actualListTypeName + actualFormatted.mkString("(", ", ", ")")
      val expectedStr = expectedListTypeName + expectedFormatted.mkString("(", ", ", ")")

      val desc = if expect.isNegated then
        s"Expected $actualStr to not equal $expectedStr"
      else
        s"Expected $actualStr to equal $expectedStr"
      expect.fail(desc)
    else expect.pass
    Future.successful(r)

  // Note: Not using IterableOnce here as it matches Option and we don't want that.
  def (expect: Expect[Iterable[T]]) toEqual[T] (expected: Iterable[T]) 
      given (
        eqq: Eq[T],
        fmt: Formatter[T]
      ): Expectation =
    new Expectation:
      def evaluate(): Future[ExpectationResult] =
        val actual = expect.evaluate()
        evalToEqual(actual, expected, expect, listTypeName(actual), listTypeName(expected))

  // We use ClassTag here to avoid "double definition error" wrt Expect[Iterable[T]]
  def (expect: Expect[Array[T]]) toEqual[T : ClassTag] (expected: Iterable[T]) 
      given (
        eqq: Eq[T],
        fmt: Formatter[T]
      ): Expectation =
    new Expectation:
      def evaluate(): Future[ExpectationResult] =
        val actual = expect.evaluate()
        evalToEqual(actual, expected, expect, "Array", listTypeName(expected))
  
  /**
   * (1, 2, 3) toHaveLength 3
   */
  def (expect: Expect[IterableOnce[T]]) toHaveLength[T] (expected: Int) given(ec: ExecutionContext): Expectation =
    new Expectation:
      def evaluate(): Future[ExpectationResult] =
        val actual = expect.evaluate()
        val actualLength = actual.iterator.size
        var r = if expect.isNegated && actualLength == expected then       expect.fail(s"Expected size *not* to be $expected but was $actualLength")
                else if expect.isNegated && actualLength != expected then  expect.pass
                else if actualLength != expected then                      expect.fail(s"Expected size to be $expected but was $actualLength")
                else                                                       expect.pass

        Future.successful(r)


  // TODO:
  // - toContain i lista (massa varianter, IterableOnce-ish)
  // - toContain i Map (immutable + mutable)
  // - toContainKey+toContainValue i Map
  // - i Jasmine: expect(x).toEqual(jasmine.objectContaining({ foo: jasmine.arrayContaining("bar") }))
  // - toContain(value | KeyConstraint | ValueConstraint)
  //     - expect(myMap).toContain(key("foo"))
  //     - expect(myMap) toContain "foo" -> 42
  //     - expect(myMap) toContain(value(42))
  //     - expect(myMap).to.contain.key(42)
}