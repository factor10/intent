
package acme

import intent._

import scala.concurrent.Future

case class MyOtherState()

class ExpectTest extends TestSuite with Intent[MyOtherState]:
  "an expectation" - :
    "can be negated" in { _ =>
      expect(1 + 2).not.toEqual(4)
    }
    "can check a Future" in { _ =>
      val f = Future { 1 + 2 }
      expect(f).toCompleteWith(3)
    }
    "can check a Seq" in { _ =>
      val s = Seq(1, 2, 3)
      expect(s).toContain(2)
    }
    "can check a List" in { _ =>
      val l = List(1, 2, 3)
      expect(l).not.toContain(4)
    }

  def emptyState = MyOtherState()
