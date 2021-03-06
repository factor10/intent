package intent.matchers

import intent.{Stateless, TestSuite}
import intent.helpers.Meta

class ToContainTest extends TestSuite with Stateless with Meta:
  "toContain":
    "a list of Int":
      "should contain element" in expect(Seq(1, 2, 3)).toContain(2)
      "should **not** contain missing element" in expect(Seq(1, 2, 3)).not.toContain(4)

    "a list of String":
      "should contain element" in expect(Seq("one", "two", "three")).toContain("two")
      "should **not** contain missing element" in expect(Seq("one", "two", "three")).not.toContain("four")

    "a list of Boolean":
      "should contain element" in expect(Seq(true, false)).toContain(false)
      "should **not** contain missing element" in expect(Seq(true)).not.toContain(false)

    "a null list":
      "should not contain an element" in expect(null.asInstanceOf[Seq[Int]]).not.toContain(42)

      "is described properly when actual is null" in:
        runExpectation(expect(null.asInstanceOf[Seq[Int]]).toContain(42),
          "Expected <null> to contain 42")

    "an infinite list":
      "can be contains-checked, but will abort" in:
        val list = LazyList.from(1)
        expect(list).not.toContain(-1)

      "can be contains-checked, but will not detect anything beyond the limit" in:
        given cutoff as intent.core.ListCutoff = intent.core.ListCutoff(5)
        val list = LazyList.from(1)
        expect(list).not.toContain(10)

    "for Map":
      "of String -> Int":
        "when negated":
          "does not contain element" in:
            expect(Map("one" -> 1, "two" -> 2)).not.toContain("three" -> 3)

          "error is described properly" in:
            runExpectation(
              expect(Map("one" -> 1, "two" -> 2)).not.toContain("one" -> 1),
              """Expected Map(...) to not contain:
              |  "one" -> 1""".stripMargin)

        "with invalid key":
          "error is described properly" in:
            runExpectation(
              expect(Map("one" -> 1, "two" -> 2)).toContain("three" -> 3),
                """Expected Map(...) to contain:
                |  "three" -> 3""".stripMargin)

        "with correct key but invalid value":
          "error is described properly" in:
          runExpectation(
            expect(Map("one" -> 1, "two" -> 2)).toContain("two" -> 3),
            """Expected Map(...) to contain:
            |  "two" -> 3 but found "two" -> 2""".stripMargin)

        "with correct key and value":
          "should contain element" in:
            expect(Map("one" -> 1, "two" -> 2)).toContain("two" -> 2)

      "for mutable scala.collection.mutable.Map":
        "with correct key and value":
          "should contain element" in:
            expect(scala.collection.mutable.Map("one" -> 1, "two" -> 2)).toContain("two" -> 2)

      "for scala.collection.immutable.Map":
        "with correct key and value":
          "should contain element" in:
            expect(scala.collection.immutable.Map("one" -> 1, "two" -> 2)).toContain("two" -> 2)
