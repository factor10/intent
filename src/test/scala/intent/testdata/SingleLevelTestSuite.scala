package intent.testdata

import intent.Stateless

class SingleLevelTestSuite extends Stateless:
  "root suite":
    "root test" in expect( 1 + 1 ).toEqual(2)
