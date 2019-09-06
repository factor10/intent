package intent.testdata

import intent.{Intent, TestSuite}

class ToHaveLengthTest extends TestSuite with Intent[Unit] {
  "toHaveLength" - {
    "empty list should have length 0" in { _ =>
      expect(Seq()).toHaveLength(0)
    }

    "Seq(1) should have length 1" in { _ =>
      expect(Seq(1)).toHaveLength(1)
    }

    "Nested Seq(Seq(), Seq()) should have length 2" in { _ =>
      expect(Seq(Seq(), Seq())).toHaveLength(2)
    }

    "Seq() should *not* have length 1" in { _ =>
      expect(Seq()).not.toHaveLength(1)
    }
  }

  def emptyState = ()
}
