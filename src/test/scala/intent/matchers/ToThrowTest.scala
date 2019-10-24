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

      "should detect wrong exception when negated" in:
        runExpectation(expect(throwIllegalState).not.toThrow[IllegalStateException](),
          "Expected the code not to throw java.lang.IllegalStateException, but it did")

      "should detect absence of exception" in:
        runExpectation(expect(dontThrow).toThrow[IllegalArgumentException](),
          "Expected the code to throw java.lang.IllegalArgumentException, but it did not throw anything")

      "should detect absence of exception when negated" in:
        expect(dontThrow).not.toThrow[IllegalArgumentException]()
      
      "should detect wrong exception and retain the exception" in:
        runExpectation(expect(throwIllegalState).toThrow[IllegalArgumentException](), {
          case t: IllegalStateException => t.getMessage == "state error"
        })

      "should find when a sub class is thrown" in expect(throwIllegalArg).toThrow[RuntimeException]()

      "should find when a sub class is thrown, when negated" in:
        runExpectation(expect(throwIllegalState).not.toThrow[RuntimeException](),
          "Expected the code not to throw java.lang.RuntimeException, but it threw java.lang.IllegalStateException")

    "with exception type and expected message":
      "should find match" in expect(throwIllegalArg).toThrow[IllegalArgumentException]("arg error")
      "should find negated match" in expect(throwIllegalArg).not.toThrow[IllegalArgumentException]("wrong text")

    "with exception type and expected RegExp message":
      "should find match" in expect(throwIllegalArg).toThrow[IllegalArgumentException]("er+".r)
      "should find negated match" in expect(throwIllegalArg).not.toThrow[IllegalArgumentException]("er[^r]")
    
  def throwIllegalArg = throw new IllegalArgumentException("arg error")
  def throwIllegalState = throw new IllegalStateException("state error")
  def dontThrow = "tada"