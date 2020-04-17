package intent.matchers

import intent.{Stateless, TestSuite}
import intent.helpers.Meta

class ToThrowTest extends TestSuite with Stateless with Meta:
  "toThrow":
    "with only exception type":
      "should find match" in expect(throwIllegalArg).toThrow[IllegalArgumentException]()

      "should find negated match" in expect(throwIllegalState).not.toThrow[IllegalArgumentException]()

      "should handle NPE" in expect(throwNPE).toThrow[NullPointerException]()

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
      "should handle NPE" in expect(throwNPE).toThrow[NullPointerException](null.asInstanceOf[String])

      "should describe when type is correct but message is not" in:
        runExpectation(expect(throwIllegalArg).toThrow[IllegalArgumentException]("something else"),
          "Expected the code to throw java.lang.IllegalArgumentException with message \"something else\", but the message was \"arg error\"")

      "should describe wrong exception+message when negated" in:
        runExpectation(expect(throwIllegalState).not.toThrow[IllegalStateException]("state error"),
          "Expected the code not to throw java.lang.IllegalStateException with message \"state error\", but it did")


    "with exception type and expected RegExp message":
      "should find match" in expect(throwIllegalArg).toThrow[IllegalArgumentException]("er+".r)
      "should find negated match" in expect(throwIllegalArg).not.toThrow[IllegalArgumentException]("er[^r]")

      "should describe when type is correct but message is not" in:
        runExpectation(expect(throwIllegalArg).toThrow[IllegalArgumentException]("^x".r),
          "Expected the code to throw java.lang.IllegalArgumentException with message matching /^x/, but the message was \"arg error\"")

  def throwIllegalArg = throw IllegalArgumentException("arg error")
  def throwIllegalState = throw IllegalStateException("state error")
  def throwNPE = throw NullPointerException()
  def dontThrow = "tada"
