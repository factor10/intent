package intent.testdata

import intent.{Intent, TestSuite}

class ToEqualTest extends TestSuite with Intent[Unit] {
  "toEqual" - {
    // TODO: Some sort of table driven tests would be a nice addition
    "for Boolean" - {
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
    }
  }

  def emptyState = ()
}
