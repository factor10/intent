package intent.testdata

import intent.State


class SingleLevelTestSuite extends State[Unit] {
  "root suite" - :
    "root test" in { state =>
      expect( 1 + 1 ) toEqual 2
    }

  def emptyState = ()
}