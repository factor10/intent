package intent.matchers

import intent.{Stateless, TestSuite}
import intent.helpers.Meta

class ToContainAllOfTest extends TestSuite with Stateless with Meta with
  "toContainAllOf":
    "for Map":
      "of String -> Int":
        "when negated":
          "error is described properly also for multiple elements" in:
            runExpectation(
              expect(Map("one" -> 1, "two" -> 2)).not.toContainAllOf("one" -> 1, "two" -> 2),
              """Expected Map(...) to not contain:
              |  one -> 1
              |  two -> 2""".stripMargin)

        "error is described properly" in:
          runExpectation(
            expect(Map("one" -> 11, "two" -> 22)).toContainAllOf("one" -> 1, "two" -> 2),
            """Expected Map(...) to contain:
            |  one -> 1 but found one -> 11
            |  two -> 2 but found two -> 22""".stripMargin)

        "with correct key and values":
          "should contain multiple elements" in:
            expect(Map("one" -> 1, "two" -> 2)).toContainAllOf("two" -> 2, "one" -> 1)

      "for mutable map":
        "with correct key and values":
          "should contain multiple elements" in:
            expect(scala.collection.mutable.Map("one" -> 1, "two" -> 2)).toContainAllOf("two" -> 2, "one" -> 1)
