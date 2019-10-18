package intent.util

import intent._
import intent.core.Expectation

import java.util.concurrent.Executors

import scala.concurrent.{Await, ExecutionContext, Promise}
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Success, Try}

class DelayedFutureTestState
  val executorService = Executors.newFixedThreadPool(1)
  given executionContext: ExecutionContext = ExecutionContext.fromExecutorService(executorService)

  val executorThreadId =
    // Determine the thread ID of the execution context, so we can compare with that in the tests.
    val p = Promise[Long]()
    executionContext.execute(() => p.success(Thread.currentThread().getId))
    Await.result(p.future, 2 seconds)

  def run(f:(given ExecutionContext) => Expectation): Expectation = f

object DelayedFutureTestState
  val instance = DelayedFutureTestState()

class DelayedFutureTest extends TestSuite with State[DelayedFutureTestState]

  "A DelayedFuture" using (DelayedFutureTestState.instance) to :

    "should run the callback in the supplied execution context" in :
      s =>
        s.run :
          val f = DelayedFuture(10 milliseconds)(Thread.currentThread().getId)
          whenComplete(f) :
            tid => expect(tid).toEqual(s.executorThreadId)

    "should be cancellable" in:
      s =>
        s.run:
          var hasRun = false
          val f = DelayedFuture(100 milliseconds) {
            hasRun = true
          }
          f.cancel()
          whenComplete(f) :
            _ => expect(hasRun).toEqual(false)

    "should have a default result after being cancelled" in:
      s =>
        s.run:
          val f = DelayedFuture(100 milliseconds) { 42 }
          f.cancel()
          whenComplete(f):
            value => expect(value).toEqual(0)

    "should be chainable" in:
      s =>
        s.run:
          val f = DelayedFuture(10 milliseconds) { 21 }.map(_ * 2)
          whenComplete(f):
            result => expect(result).toEqual(42)

    "should be awaitable with result" in:
      s =>
        s.run:
          val f = DelayedFuture(10 milliseconds) { 42 }
          val result = Await.result(f, 100 milliseconds)
          expect(result).toEqual(42)

    "should be awaitable without result" in:
      s =>
        s.run:
          val f = DelayedFuture(10 milliseconds) { 42 }
          val f2 = Await.ready(f, 100 milliseconds)
          expect[Option[Try[Int]]](f2.value).toEqual(Some(Success(42)))
