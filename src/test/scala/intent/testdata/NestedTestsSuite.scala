package intent.testdata

import intent.Stateless

class NestedTestsSuite extends Stateless:
  "root suite":
    "child suite":
      "grand child suite":
        "grand child test" in expect( 1 + 1 ).toEqual(2)

      "child test" in expect( 1 + 1 ).toEqual(2)

    "root test" in expect( 1 + 1 ).toEqual(2)
