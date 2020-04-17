package intent.matchers

import intent._

import scala.concurrent.Future

class ExpectTest extends TestSuite with Stateless:
  "an expectation":
    "can be negated" in expect(1 + 2).not.toEqual(4)

    "can check a Future" in:
      val f = Future { 1 + 2 }
      expect(f).toCompleteWith(3)

    "can check a Seq" in:
      val s = Seq(1, 2, 3)
      expect(s).toContain(2)

    "can check a List" in:
      val l = List(1, 2, 3)
      expect(l).not.toContain(4)

    "can pass using successful" in success()
