package intent.matchers

import intent.{Stateless, TestSuite}

class ToEqualTest extends TestSuite with Stateless:
  "toEqual" - :
    // TODO: Some sort of table driven tests would be a nice addition
    "for Boolean" - :
      "true should equal true" in expect(true).toEqual(true)

      "true should *not* equal false" in expect(true).not.toEqual(false)

      "false should equal false" in expect(false).toEqual(false)

      "false should *note* equal true" in expect(false).not.toEqual(true)

    "for String" - :
      "<empty> should equal <empty>" in expect("").toEqual("")

      "<foo> should equal <foo>" in expect("foo").toEqual("foo")

      "<foo> should *not* equal <bar>" in expect("foo").not.toEqual("bar")

      "<🤓> should equal <🤓>" in expect("🤓").toEqual("🤓")
