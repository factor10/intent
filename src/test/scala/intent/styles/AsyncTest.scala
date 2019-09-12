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

class AsyncStateTest extends TestSuite with AsyncIntent[String] with Meta:

  "a test with async state" - :

    "when we map on the state" via mapString - :
      "sees the appropriate state" in :
        state => expect(state).toEqual("Hello world")

    "when we async-map on the state" via mapStringAsync - :
      "sees the appropriate state" in :
        state => expect(state).toEqual("Hello async world")

  def mapString(s: String): String = s + " world"
  def mapStringAsync(s: String, dummy: Int): Future[String] = Future { s + " async world" }
  def emptyState = Future { "Hello" }