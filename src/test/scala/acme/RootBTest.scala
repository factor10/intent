package acme

import intent._

class RootBTest extends TestSuite with Intent[Unit] {
  "Root B Test" - {
    "can add" in { _ => expect(1 + 2) toEqual 3 }
  }

  def emptyState: Unit = ()
}

class RootB2Test extends TestSuite with Intent[Unit] {
  "Root B2 Test" - {
    "can add" in { _ => expect(1 + 2) toEqual 3 }
  }

  def emptyState: Unit = ()
}
