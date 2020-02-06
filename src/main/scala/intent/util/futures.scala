package intent.util

import java.util.concurrent.atomic.AtomicBoolean
import java.util.{Timer, TimerTask}

import scala.concurrent.duration.Duration
import scala.concurrent._
import scala.util.Try

trait DelayedFuture[T] extends Future[T] with
  def cancel(): Unit

// From: http://stackoverflow.com/questions/16359849/scala-scheduledfuture
object DelayedFuture with
  private val timer = new Timer

  private def makeTask[T](body: => T)(schedule: TimerTask => Unit)(using ctx: ExecutionContext): Future[T] =
    val prom = Promise[T]()
    schedule(
      new TimerTask {
        override def run() =
          // IMPORTANT: The timer task just starts the execution on the passed
          // ExecutionContext and is thus almost instantaneous (making it
          // practical to use a single  Timer - hence a single background thread).
          ctx.execute(() => prom.complete(Try(body)))
      }
    )
    prom.future

  def apply[T](duration: Duration)(body: => T)(using ctx: ExecutionContext): DelayedFuture[T] =
    val isCancelled = new AtomicBoolean(false)
    val f = makeTask({
      if (!isCancelled.get()) body else null.asInstanceOf[T]
    })(timer.schedule(_, duration.toMillis))
    DelayedFutureImpl(f, () => isCancelled.set(true))

  private class DelayedFutureImpl[T](val inner: Future[T], cancelFun: () => Unit) extends DelayedFuture[T] with
    override def cancel(): Unit = cancelFun()

    override def onComplete[U](f: (Try[T]) => U)(implicit executor: ExecutionContext): Unit = inner.onComplete(f)

    override def isCompleted: Boolean = inner.isCompleted

    override def value: Option[Try[T]] = inner.value

    override def ready(atMost: Duration)(implicit permit: CanAwait): DelayedFutureImpl.this.type =
      inner.ready(atMost)
      this

    override def result(atMost: Duration)(implicit permit: CanAwait): T = inner.result(atMost)

    override def transform[S](f: Try[T] => Try[S])(implicit executor: ExecutionContext): Future[S] =
      inner.transform(f)

    override def transformWith[S](f: Try[T] => Future[S])(implicit executor: ExecutionContext): Future[S] =
      inner.transformWith(f)
