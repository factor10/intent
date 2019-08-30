package acme

import intent._

class RootBTest extends Intent[Unit] {
  "Root B Test" - {
    "can add" in { _ => expect(1 + 2) toEqual 3 }
  }
}

class RootB2Test extends Intent[Unit] {
  "Root B2 Test" - {
    "can add" in { _ => expect(1 + 2) toEqual 3 }
  }
}
