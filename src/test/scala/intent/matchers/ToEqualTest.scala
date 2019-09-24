package intent.matchers

import intent.{Stateless, TestSuite}

class ToEqualTest extends TestSuite with Stateless:
  "toEqual" :

    "for Boolean" :
      "true should equal true" in expect(true).toEqual(true)

      "true should *not* equal false" in expect(true).not.toEqual(false)

      "false should equal false" in expect(false).toEqual(false)

      "false should *note* equal true" in expect(false).not.toEqual(true)

    "for String" :
      "<empty> should equal <empty>" in expect("").toEqual("")

      "<foo> should equal <foo>" in expect("foo").toEqual("foo")

      "<foo> should *not* equal <bar>" in expect("foo").not.toEqual("bar")

      "<🤓> should equal <🤓>" in expect("🤓").toEqual("🤓")

    "for Char" :
      "should support equality test" in expect('a').toEqual('a')
      "should support inequality test" in expect('a').not.toEqual('A')

    "for Double" :
      "should support equality test" in expect(3.14d).toEqual(3.14d)
      "should support inequality test" in expect(3.14d).not.toEqual(2.72d)

      "with precision" :
        "should test equal with 12 decimals" in expect(1.123456789123d).toEqual(1.123456789123d)
        "should allow diff in the 13th decimal" in expect(1.1234567891234d).toEqual(1.1234567891235d)
        "should allow customization of precision" in:
          given customPrecision as intent.core.FloatingPointPrecision[Double] :
            def numberOfDecimals: Int = 2
          expect(1.234d).toEqual(1.235d)

    "for Float" :
      "should support equality test" in expect(3.14f).toEqual(3.14f)
      "should support inequality test" in expect(3.14f).not.toEqual(2.72f)

      "with precision" :
        "should test equal with 6 decimals" in expect(1.123456f).toEqual(1.123456f)
        "should allow diff in the 7th decimal" in expect(1.1234567f).toEqual(1.1234568f)
        "should allow customization of precision" in:
          given customPrecision as intent.core.FloatingPointPrecision[Float] :
            def numberOfDecimals: Int = 2
          expect(1.234f).toEqual(1.235f)

    "for Option" :
      "Some should equal Some" in expect(Some(42)).toEqual(Some(42))
      "Some should test inner equality" in expect(Some(42)).not.toEqual(Some(43))
      "Some should not equal None" in expect(Some(42)).not.toEqual(None)
      "None should equal None" in expect[Option[String]](None).toEqual(None)
      "should consider custom equality" in :
        given customIntEq as intent.core.Eq[Int] :
          def areEqual(a: Int, b: Int) = Math.abs(a - b) == 1
        expect(Some(42)).toEqual(Some(43))