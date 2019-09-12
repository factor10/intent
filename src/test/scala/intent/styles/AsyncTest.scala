package intent.styles

import intent._
import scala.concurrent.Future

class AsyncTest extends TestSuite with IntentStateless:

  "an async test" :
    "can use whenComplete" in :
      val f = Future { 21 * 2 }
      whenComplete(f):
        result => expect(result).toEqual(42)
