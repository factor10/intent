package intent.matchers

import intent.{Stateless, TestSuite}

class ToHaveLengthTest extends TestSuite with Stateless:
  "toHaveLength":
    "empty list should have length 0" in expect(Seq()).toHaveLength(0)

    "Seq(1) should have length 1" in expect(Seq(1)).toHaveLength(1)

    "Nested Seq(Seq(), Seq()) should have length 2" in expect(Seq(Seq(), Seq())).toHaveLength(2)

    "Seq() should *not* have length 1" in expect(Seq()).not.toHaveLength(1)
