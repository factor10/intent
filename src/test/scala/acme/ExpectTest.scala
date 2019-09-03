
package acme

import intent._

case class MyOtherState()

class ExpectTest extends IntentMaker with Intent[MyOtherState] {
  "an expectation" - {
    "can be negated" in { _ =>
      expect(1 + 2).not.toEqual(4)
    }
  }

  def emptyState = MyOtherState()
}
