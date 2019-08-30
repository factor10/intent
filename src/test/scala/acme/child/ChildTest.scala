package acme.child

import intent._

class ChildTest extends Intent[Unit] {
  "Child Test" - {
    "can add" in { _ => expect(1 + 2) toEqual 3 }
  }
}
