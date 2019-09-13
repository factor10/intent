package intent.testdata

import intent.State

class NestedTestsSuite extends State[Unit] {
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