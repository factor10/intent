package intent.matchers

import intent.{Stateless, TestSuite}

class ToThrowTest extends TestSuite with Stateless
  "toThrow":
    "with only exception type":
      "should find match" in expect(throwIllegalArg).toThrow[IllegalArgumentException]()

      "should find negated match" in expect(throwIllegalState).not.toThrow[IllegalArgumentException]()

  def throwIllegalArg = throw new IllegalArgumentException("arg error")
  def throwIllegalState = throw new IllegalStateException("state error")

  // TODO: Test sub class also.. 