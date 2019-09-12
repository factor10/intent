package intent.testdata

import intent.Intent


class SingleLevelTestSuite extends Intent[Unit] {
  "root suite" - :
    "root test" in { state =>
      expect( 1 + 1 ) toEqual 2
    }

  def emptyState = ()
}