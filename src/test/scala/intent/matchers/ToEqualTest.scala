package intent.matchers

import intent.{Stateless, TestSuite}
import scala.util.{Success, Failure, Try}
import intent.helpers.Meta

class ToEqualTest extends TestSuite with Stateless with Meta:
  "toEqual":

    "for Boolean":
      "true should equal true" in expect(true).toEqual(true)

      "true should *not* equal false" in expect(true).not.toEqual(false)

      "false should equal false" in expect(false).toEqual(false)

      "false should *note* equal true" in expect(false).not.toEqual(true)

    "for String":
      "<empty> should equal <empty>" in expect("").toEqual("")

      "<foo> should equal <foo>" in expect("foo").toEqual("foo")

      "<foo> should *not* equal <bar>" in expect("foo").not.toEqual("bar")

      "<🤓> should equal <🤓>" in expect("🤓").toEqual("🤓")

      "handles <null> as expected" in expect("").not.toEqual(null.asInstanceOf[String])

      "handles <null> as actual" in expect(null.asInstanceOf[String]).not.toEqual("")

    "for Char":
      "should support equality test" in expect('a').toEqual('a')
      "should support inequality test" in expect('a').not.toEqual('A')

    "for Double":
      "should support equality test" in expect(3.14d).toEqual(3.14d)
      "should support inequality test" in expect(3.14d).not.toEqual(2.72d)

      "with precision":
        "should test equal with 12 decimals" in expect(1.123456789123d).toEqual(1.123456789123d)
        "should allow diff in the 13th decimal" in expect(1.1234567891234d).toEqual(1.1234567891235d)
        "should allow customization of precision" in:
          given customPrecision as intent.core.FloatingPointPrecision[Double]:
            def numberOfDecimals: Int = 2
          expect(1.234d).toEqual(1.235d)

    "for Float":
      "should support equality test" in expect(3.14f).toEqual(3.14f)
      "should support inequality test" in expect(3.14f).not.toEqual(2.72f)

      "with precision":
        "should test equal with 6 decimals" in expect(1.123456f).toEqual(1.123456f)
        "should allow diff in the 7th decimal" in expect(1.1234567f).toEqual(1.1234568f)
        "should allow customization of precision" in:
          given customPrecision as intent.core.FloatingPointPrecision[Float]:
            def numberOfDecimals: Int = 2
          expect(1.234f).toEqual(1.235f)

    "for Option":
      "Some should equal Some" in expect(Some(42)).toEqual(Some(42))
      "Some should test inner equality" in expect(Some(42)).not.toEqual(Some(43))
      "Some should not equal None" in expect(Some(42)).not.toEqual(None)
      "None should equal None" in expect[Option[String]](None).toEqual(None)
      "should consider custom equality" in:
        given customIntEq as intent.core.Eq[Int]:
          def areEqual(a: Int, b: Int) = Math.abs(a - b) == 1
        expect(Some(42)).toEqual(Some(43))

    "for Try":
      "Success should equal Success" in expect[Try[Int]](Success(42)).toEqual(Success(42))
      "Success should test inner equality" in expect[Try[Int]](Success(42)).not.toEqual(Success(43))
      "Success should not equal Failure" in expect[Try[Int]](Success(42)).not.toEqual(Failure(new Exception("oops")))
      "Failure should equal Failure" in:
        val ex = RuntimeException("oops")
        expect[Try[Int]](Failure(ex)).toEqual(Failure(ex))
      "Failure should test inner equality" in:
        val ex1 = RuntimeException("oops1")
        val ex2 = RuntimeException("oops2")
        expect[Try[Int]](Failure(ex1)).not.toEqual(Failure(ex2))

    "for Long":
      "should support equality test" in expect(10L).toEqual(10L)
      "should support inequality test" in expect(10L).not.toEqual(11L)

    "for collection":
      "supports equality test" in expect(Seq(1, 2)).toEqual(Seq(1, 2))
      "detects inquality in shorter length" in expect(Seq(1, 2)).not.toEqual(Seq(1))
      "detects inquality in longer length" in expect(Seq(1, 2)).not.toEqual(Seq(1, 2, 3))
      "detects inquality in item" in expect(Seq(1, 2)).not.toEqual(Seq(1, 3))
      "supports equality with same LazyList" in:
        val list = LazyList.from(1)
        expect(list).toEqual(list)

      "handles <null> as expected" in expect(Seq.empty[Int]).not.toEqual(null.asInstanceOf[Seq[Int]])

      "handles <null> as actual" in expect(null.asInstanceOf[Seq[Int]]).not.toEqual(Seq.empty[Int])

      "is described properly when actual is null" in:
        runExpectation(expect(null.asInstanceOf[Seq[Int]]).toEqual(Seq(1, 2, 3)),
          "Expected <null> to equal List(1, 2, 3)")

      "is described properly when expected is null" in:
        runExpectation(expect(Seq(1, 2, 3)).toEqual(null.asInstanceOf[Seq[Int]]),
          "Expected List(1, 2, 3) to equal <null>")

      "is described properly when both are null" in:
        runExpectation(expect(null.asInstanceOf[Seq[Int]]).not.toEqual(null.asInstanceOf[Seq[Int]]),
          "Expected <null> to not equal <null>")

    "for array":
      "supports equality test" in expect(Array(1, 2)).toEqual(Array(1, 2))
      "detects inquality in item" in expect(Array(1, 2)).not.toEqual(Array(1, 3))
      "supports equality test with non-array" in expect(Array(1, 2)).toEqual(Seq(1, 2))
