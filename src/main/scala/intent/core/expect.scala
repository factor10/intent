package intent.core

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Failure}
import scala.collection.IterableOnce
import scala.collection.mutable.ListBuffer
import scala.language.implicitConversions
import scala.util.matching.Regex
import scala.reflect.ClassTag

import intent.core.{Expectation, ExpectationResult, TestPassed, TestFailed, PositionDescription, MapLike}
import intent.macros.Position
import intent.util.DelayedFuture
import intent.core.expectations._

/**
  * Defines cutoff limit for list comparisons (equality as well as contains), in order to make it
  * possible to use `toEqual` and `toContain` with infinite lists.
  *
  * @param maxItems determines how many items of the list to check before giving up
  * @param printItems the number of items to print in case of a "cutoff abort"
  */
case class ListCutoff(maxItems: Int = 1000, printItems: Int = 5)

class CompoundExpectation(inner: Seq[Expectation])(using ec: ExecutionContext) extends Expectation with
  def evaluate(): Future[ExpectationResult] =
    val innerFutures = inner.map(_.evaluate())
    Future.sequence(innerFutures).map { results =>
      // any failure => failure
      ???
    }

class Expect[T](blk: => T, position: Position, negated: Boolean = false) with
  import PositionDescription._

  def evaluate(): T = blk
  def isNegated: Boolean = negated
  def negate(): Expect[T] = new Expect(blk, position, !negated)

  def fail(desc: String): ExpectationResult               = TestFailed(position.contextualize(desc), None)
  def fail(desc: String, t: Throwable): ExpectationResult = TestFailed(position.contextualize(desc), Some(t))
  def pass: ExpectationResult                             = TestPassed()

trait ExpectGivens with

  given defaultListCutoff: ListCutoff = ListCutoff()

  def [T](expect: Expect[T]) not: Expect[T] = expect.negate()

  def [T](expect: Expect[T]) toEqual (expected: T)(using eqq: Eq[T], fmt: Formatter[T]): Expectation =
    new EqualExpectation(expect, expected)

  // toMatch is partial
  def [T](expect: Expect[String]) toMatch (re: Regex)(using fmt: Formatter[String]): Expectation =
    new MatchExpectation(expect, re)

  def [T](expect: Expect[Future[T]]) toCompleteWith (expected: T)
     (using
        eqq: Eq[T],
        fmt: Formatter[T],
        errFmt: Formatter[Throwable],
        ec: ExecutionContext,
        timeout: TestTimeout
      ): Expectation =
    new ToCompleteWithExpectation(expect, expected)

  // We use ClassTag here to avoid "double definition error" wrt Expect[IterableOnce[T]]
  def [T : ClassTag](expect: Expect[Array[T]]) toContain (expected: T)
     (using
        eqq: Eq[T],
        fmt: Formatter[T],
        cutoff: ListCutoff
      ): Expectation =
    new ArrayContainExpectation(expect, expected)

  def [T](expect: Expect[IterableOnce[T]]) toContain (expected: T)
     (using
        eqq: Eq[T],
        fmt: Formatter[T],
        cutoff: ListCutoff
      ): Expectation =
    new IterableContainExpectation(expect, expected)

  /**
   * Expect that a key-value tuple exists in the given map
   */
  def [K, V](expect: Expect[MapLike[K, V]]) toContain (expected: (K, V))
    (using
       eqq: Eq[V],
       keyFmt: Formatter[K],
       valueFmt: Formatter[V]
    ): Expectation = new MapContainExpectation(expect, Seq(expected))

  /**
   * Expect that multiple key-value tuple exists in the given map
   */
  def [K, V](expect: Expect[MapLike[K, V]]) toContainAllOf (expected: (K, V)*)
    (using
      eqq: Eq[V],
      keyFmt: Formatter[K],
      valueFmt: Formatter[V]
    ): Expectation = new MapContainExpectation(expect, expected)

  // Note: Not using IterableOnce here as it matches Option and we don't want that.
  def [T](expect: Expect[Iterable[T]]) toEqual (expected: Iterable[T])
     (using
        eqq: Eq[T],
        fmt: Formatter[T]
      ): Expectation =
    new IterableEqualExpectation(expect, expected)

  // We use ClassTag here to avoid "double definition error" wrt Expect[Iterable[T]]
  def [T : ClassTag](expect: Expect[Array[T]]) toEqual (expected: Iterable[T])
     (using
        eqq: Eq[T],
        fmt: Formatter[T]
      ): Expectation =
    new ArrayEqualExpectation(expect, expected)

  /**
   * (1, 2, 3) toHaveLength 3
   */
  def [T](expect: Expect[IterableOnce[T]]) toHaveLength (expected: Int)(using ec: ExecutionContext): Expectation =
    new LengthExpectation(expect, expected)

  // toThrow with only exception type
  def [TEx : ClassTag](expect: Expect[_]) toThrow ()(using fmt: Formatter[String]): Expectation =
    new ThrowExpectation[TEx](expect, AnyExpectedMessage)

  // toThrow with exception type + message (string, so full match)
  def [TEx : ClassTag](expect: Expect[_]) toThrow (expectedMessage: String)(using fmt: Formatter[String]): Expectation =
    new ThrowExpectation[TEx](expect, ExactExpectedMessage(expectedMessage))

    // toThrow with exception type + regexp (partial match, like toMatch)
  def [TEx : ClassTag](expect: Expect[_]) toThrow (re: Regex)(using fmt: Formatter[String]): Expectation =
    new ThrowExpectation[TEx](expect, RegexExpectedMessage(re))

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
