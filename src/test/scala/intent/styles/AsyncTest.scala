package intent.styles

import intent._
import helpers.Meta
import scala.concurrent.Future

class AsyncTest extends TestSuite with IntentStateless with Meta:

  "an async test" :
    "can use whenComplete" in :
      val f = Future { 21 * 2 }
      whenComplete(f):
        result => expect(result).toEqual(42)

    "fails with failure in whenComplete" in :
      runExpectation({
        val f = Future[Int] { throw new RuntimeException("async failure") }
        whenComplete(f):
          result => expect(result).toEqual(42)
      }, "The Future passed to 'whenComplete' failed")
