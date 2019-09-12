package intent.styles

import intent._

class StatelessTest extends TestSuite with IntentStateless:
  "a stateless test" - :
    "works well" in (expect(1) toEqual 1)

  "a stateless test without minus/hyphen" :
    "also works well" in (expect(1) toEqual 1)
