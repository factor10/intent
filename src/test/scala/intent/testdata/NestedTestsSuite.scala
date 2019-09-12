package intent.testdata

import intent.Intent

class NestedTestsSuite extends Intent[Unit] {
  "root suite" - :
    "child suite" - :
      "grand child suite" - :
        "grand child test" in { state =>
          expect( 1 + 1 ) toEqual 2
        }

      "child test" in { state =>
        expect( 1 + 1 ) toEqual 2
      }

    "root test" in { state =>
      expect( 1 + 1 ) toEqual 2
    }

  def emptyState = ()
}