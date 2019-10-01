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
