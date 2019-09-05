package acme.child

import intent._

class ChildTest extends TestSuite with Intent[Unit] {
  "Child Test" - {
    "can add" in { _ => expect(1 + 2) toEqual 3 }
  }

  def emptyState: Unit = ()
}
