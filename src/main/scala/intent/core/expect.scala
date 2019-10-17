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

  given defaultListCutoff as ListCutoff = ListCutoff()

  def (expect: Expect[T]) not[T]: Expect[T] = expect.negate()

  def (expect: Expect[T]) toEqual[T] (expected: T) given (eqq: Eq[T], fmt: Formatter[T]): Expectation =
    new EqualExpectation(expect, expected)

  // toMatch is partial
  def (expect: Expect[String]) toMatch[T] (re: Regex) given (fmt: Formatter[String]): Expectation =
    new MatchExpectation(expect, re)

  def (expect: Expect[Future[T]]) toCompleteWith[T] (expected: T) 
      given (
        eqq: Eq[T], 
        fmt: Formatter[T], 
        errFmt: Formatter[Throwable], 
        ec: ExecutionContext,
        timeout: TestTimeout
      ): Expectation =
    new ToCompleteWithExpectation(expect, expected)

  // We use ClassTag here to avoid "double definition error" wrt Expect[IterableOnce[T]]
  def (expect: Expect[Array[T]]) toContain[T : ClassTag] (expected: T)
      given (
        eqq: Eq[T],
        fmt: Formatter[T],
        cutoff: ListCutoff
      ): Expectation =
    new ArrayContainExpectation(expect, expected)

  def (expect: Expect[IterableOnce[T]]) toContain[T] (expected: T) 
      given (
        eqq: Eq[T],
        fmt: Formatter[T],
        cutoff: ListCutoff
      ): Expectation =
    new IterableContainExpectation(expect, expected)

  // Note: Not using IterableOnce here as it matches Option and we don't want that.
  def (expect: Expect[Iterable[T]]) toEqual[T] (expected: Iterable[T]) 
      given (
        eqq: Eq[T],
        fmt: Formatter[T]
      ): Expectation =
    new IterableEqualExpectation(expect, expected)

  // We use ClassTag here to avoid "double definition error" wrt Expect[Iterable[T]]
  def (expect: Expect[Array[T]]) toEqual[T : ClassTag] (expected: Iterable[T]) 
      given (
        eqq: Eq[T],
        fmt: Formatter[T]
      ): Expectation =
    new ArrayEqualExpectation(expect, expected)
  
  /**
   * (1, 2, 3) toHaveLength 3
   */
  def (expect: Expect[IterableOnce[T]]) toHaveLength[T] (expected: Int) given(ec: ExecutionContext): Expectation =
    new LengthExpectation(expect, expected)


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