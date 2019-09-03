package intent

import scala.concurrent.{ExecutionContext, Future}

trait Expectation {
  def evaluate(): Future[ExpectationResult]
}

sealed trait ExpectationResult
case class TestPassed() extends ExpectationResult
case class TestFailed(output: String) extends ExpectationResult

class CompoundExpectation(inner: Seq[Expectation]) given (ec: ExecutionContext) extends Expectation {
  def evaluate(): Future[ExpectationResult] = {
    val innerFutures = inner.map(_.evaluate())
    Future.sequence(innerFutures).map { results =>
      // any failure => failure
      ???
    }
  }
}

class AssertionError(msg: String) extends RuntimeException(msg)

class Expect[T](blk: => T, negated: Boolean = false) {
  def evaluate(): T = blk

  def negate(): Expect[T] = new Expect(blk, !negated)
}

trait ExpectGivens {

  def (expect: Expect[T]) not[T]: Expect[T] = expect.negate()

  def (expect: Expect[T]) toEqual[T] (expected: T) given (eqq: Eq[T], fmt: Formatter[T]): Expectation = {
    new Expectation {
      def evaluate(): Future[ExpectationResult] = {
        // TODO: hantera expect.negated
        val actual = expect.evaluate()
        val r = if (!eqq.areEqual(actual, expected)) {
          val actualStr = fmt.format(actual)
          val expectedStr = fmt.format(expected)
          //throw new AssertionError(s"Expected $expectedStr but got $actualStr")
          TestFailed(s"Expected $expectedStr but got $actualStr")
        } else TestPassed()
        Future.successful(r)
      }
    }
  }

  def (expect: Expect[Future[T]]) toCompleteWith[T] (expected: T) given (eqq: Eq[T], fmt: Formatter[T], ec: ExecutionContext): Expectation = {
    new Expectation {
      def evaluate(): Future[ExpectationResult] = {
        expect.evaluate().flatMap { actual =>
          new Expect(actual).toEqual(expected).evaluate()
        }
      }
    }
  }

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