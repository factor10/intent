package intent.styles

import intent._
import helpers.Meta
import scala.concurrent.{Future, ExecutionContext}

class AsyncTest extends TestSuite with Stateless with Meta:

  "an async test":
    "can use whenComplete" in:
      val f = Future { 21 * 2 }
      whenComplete(f):
        result => expect(result).toEqual(42)

    "fails with failure in whenComplete" in:
      runExpectation({
        val f = Future[Int] { throw new RuntimeException("async failure") }
        whenComplete(f):
          result => expect(result).toEqual(42)
      }, "The Future passed to 'whenComplete' failed")

class AsyncStateTest extends TestSuite with AsyncState[MyAsyncState] with Meta:

  "a test with async state" usingAsync Future{MyAsyncState("Hello")} to:
    "can map on the state" using (_.map(" world")) to:
      "and sees the appropriate state" in:
        state => expect(state.s).toEqual("Hello world")

    "can async-map on the state" usingAsync (_.asyncMap(" async world")) to:
      "sees the appropriate state" in:
        state => expect(state.s).toEqual("Hello async world")

  "a test with initially sync state" using MyAsyncState("Hello") to:
    "can async-map on the state" usingAsync (_.asyncMap(" async world")) to:
      "sees the appropriate state" in:
        state => expect(state.s).toEqual("Hello async world")

case class MyAsyncState(s: String)(using ExecutionContext):
  def map(s2: String) = copy(s = s + s2)
  def asyncMap(s2: String) = Future { copy(s = s + s2) }
