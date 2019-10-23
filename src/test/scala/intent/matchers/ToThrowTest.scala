package intent.matchers

import intent.{Stateless, TestSuite}
import intent.helpers.Meta

class ToThrowTest extends TestSuite with Stateless with Meta
  "toThrow":
    "with only exception type":
      "should find match" in expect(throwIllegalArg).toThrow[IllegalArgumentException]()

      "should find negated match" in expect(throwIllegalState).not.toThrow[IllegalArgumentException]()

      "should detect wrong exception" in:
        runExpectation(expect(throwIllegalState).toThrow[IllegalArgumentException](),
          "Expected the code to throw java.lang.IllegalArgumentException, but it threw java.lang.IllegalStateException")


  def throwIllegalArg = throw new IllegalArgumentException("arg error")
  def throwIllegalState = throw new IllegalStateException("state error")

  // TODO: Test sub class also.. 