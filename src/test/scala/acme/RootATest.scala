package acme

import intent._

class RootATest extends IntentMaker with Intent[Unit] {
  "Root A Test" - {
    "can add" in { _ => expect(1 + 2) toEqual 3 }
  }

  def emptyState: Unit = ()
}
