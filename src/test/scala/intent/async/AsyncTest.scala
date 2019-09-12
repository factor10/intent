package intent.async

import intent._
import scala.concurrent.Future

class AsyncTest extends TestSuite with Intent[Unit]:

  "an async test" - :
    "can use whenComplete" in :
      _ =>
        val f = Future { 21 * 2 }
        whenComplete(f):
          result => expect(result).toEqual(42)

  def emptyState = ()