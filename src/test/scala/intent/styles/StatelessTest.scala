package intent.styles

import intent._

class StatelessTest extends TestSuite with Stateless:
  "a stateless test":
    "works well" in (expect(1) toEqual 1)
