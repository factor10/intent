
package intent.matchers

import intent._
import intent.helpers.Meta
import intent.core.TestTimeout
import scala.concurrent.{Future, Promise}
import scala.concurrent.duration._

class FailureTest extends TestSuite with Stateless with Meta:
  "a toEqual failure":
    "is described properly" in:
      runExpectation(expect(1).toEqual(2), "Expected 2 but found 1")

    "is described properly in the negative" in:
      runExpectation(expect(1).not.toEqual(1), "Expected 1 not to equal 1")

    "is described properly with Option" in:
      runExpectation(expect(Some(1)).toEqual(None), "Expected None but found Some(1)")

    "is described properly for Iterables, when actual is longer" in:
      runExpectation(expect(Seq(1, 2)).toEqual(Seq(1)),
        "Expected List(1, 2) to equal List(1)")

    "is described properly for Iterables, when actual is shorter" in:
      runExpectation(expect(Seq(1, 2)).toEqual(Seq(1, 2, 3)),
        "Expected List(1, 2) to equal List(1, 2, 3)")

    "is described properly for Arrays" in:
      runExpectation(expect(Array(1, 2)).toEqual(Array(1)),
        "Expected Array(1, 2) to equal Array(1)")

    "is described properly for Array-Iterable" in:
      runExpectation(expect(Array(1, 2)).toEqual(Seq(1)),
        "Expected Array(1, 2) to equal List(1)")

    "with custom formatting" in:
      given customIntFmt as core.Formatter[Int]:
        def format(a: Int): String = a.toString.replace("4", "forty-")
      runExpectation(expect(42).toEqual(43),
        "Expected forty-3 but found forty-2")

  "a toMatch failure":
    "is described properly" in:
      runExpectation(expect("foobar").toMatch("^bar".r), "Expected \"foobar\" to match /^bar/")

    "is described properly in the negative" in:
      runExpectation(expect("foobar").not.toMatch("^foo".r), "Expected \"foobar\" not to match /^foo/")

  "a toContain failure":
    "is described properly" in:
      runExpectation(expect(Seq(1, 2)).toContain(3), "Expected List(1, 2) to contain 3")

    "is described properly in the negative" in:
      runExpectation(expect(Seq(1, 2)).not.toContain(1), "Expected List(1, ...) not to contain 1")

    "is described properly for Array" in:
      runExpectation(expect(Array(1, 2)).toContain(3), "Expected Array(1, 2) to contain 3")

    "is described properly when item is found in infinite stream but it's not expected" in:
      val s = LazyList.from(1)
      runExpectation(expect(s).not.toContain(4), "Expected LazyList(1, 2, 3, 4, ...) not to contain 4")

    "is described properly when item is not found in infinite stream but it's expected" in:
      val s = LazyList.from(1)
      runExpectation(expect(s).toContain(-1), "Expected LazyList(1, 2, 3, 4, 5, ...) to contain -1")

  "using fail()":
    "should fail with the given description" in runExpectation(fail("Manually failed"), "Manually failed")

  "Future timeout":
    "should abort a long-running test when whenComplete is used" in:
      given customTimeout as TestTimeout = TestTimeout(50.millis)
      val p = Promise[Int]()
      runExpectation({
        whenComplete(p.future):
          result => expect(result).toEqual(42)
      }, "Test timed out")

    "should abort a long-running test when toCompleteWith is used" in:
      given customTimeout as TestTimeout = TestTimeout(50.millis)
      val p = Promise[Int]()
      runExpectation({
        expect(p.future).toCompleteWith(42)
      }, "Test timed out")
