package intent.matchers

import intent.{Stateless, TestSuite}

class ToContainTest extends TestSuite with Stateless:
  "toContain" :
    "a list of Int" :
      "should contain element" in expect(Seq(1, 2, 3)).toContain(2)
      "should **not** contain missing element" in expect(Seq(1, 2, 3)).not.toContain(4)

    "a list of String" :
      "should contain element" in expect(Seq("one", "two", "three")).toContain("two")
      "should **not** contain missing element" in expect(Seq("one", "two", "three")).not.toContain("four")

    "a list of Boolean" :
      "should contain element" in expect(Seq(true, false)).toContain(false)
      "should **not** contain missing element" in expect(Seq(true)).not.toContain(false)

    "an infinite list" :
      "can be contains-checked, but will abort" in:
        val list = LazyList.from(1)
        expect(list).not.toContain(-1)

      "can be contains-checked, but will not detect anything beyond the limit" in:
        given cutoff as intent.core.ListCutoff = intent.core.ListCutoff(5)
        val list = LazyList.from(1)
        expect(list).not.toContain(10)