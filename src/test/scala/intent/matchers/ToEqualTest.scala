package intent.matchers

import intent.{Intent, TestSuite}

class ToEqualTest extends TestSuite with Intent[Unit]:
  "toEqual" - :
    // TODO: Some sort of table driven tests would be a nice addition
    "for Boolean" - :
      "true should equal true" in { _ =>
        expect(true).toEqual(true)
      }

      "true should *not* equal false" in { _ =>
        expect(true).not.toEqual(false)
      }

      "false should equal false" in { _ =>
        expect(false).toEqual(false)
      }

      "false should *note* equal true" in { _ =>
        expect(false).not.toEqual(true)
      }

    "for String" - :
      "<empty> should equal <empty>" in { _ =>
        expect("").toEqual("")
      }

      "<foo> should equal <foo>" in { _ =>
        expect("foo").toEqual("foo")
      }

      "<foo> should *not* equal <bar>" in { _ =>
        expect("foo").not.toEqual("bar")
      }

      "<🤓> should equal <🤓>" in { _ =>
        expect("🤓").toEqual("🤓")
      }

  def emptyState = ()
