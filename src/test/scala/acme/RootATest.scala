package acme

import intent._

class RootATest extends Intent[Unit] {
  "Root A Test" - {
    "can add" in { _ => expect(1 + 2) toEqual 3 }
  }
}
