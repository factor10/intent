
package acme

import intent._

import scala.concurrent.Future

case class MyOtherState()

class ExpectTest extends IntentMaker with Intent[MyOtherState] {
  "an expectation" - {
    "can be negated" in { _ =>
      expect(1 + 2).not.toEqual(4)
    }
    "can check a Future" in { _ =>
      val f = Future { 1 + 2 }
      expect(f).toCompleteWith(3)
    }
  }

  def emptyState = MyOtherState()
}
