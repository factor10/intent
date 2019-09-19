package intent.core

//
// Very basic implementation of the Observable pattern used to implement
// reporters, etc.
//
// Currently no need for backpressure or other fancy features, thus no dependency
// to any Reactive Stream library.

trait Observable[T]:
  def subscribe(subscriber: Subscriber[T]): Unit
  protected def publish(event: T): Unit

trait Subscriber[T]:
  // TOOD: Add onComplete to signal when it is safe to stop consuming events
  def onNext(event: T): Unit

/**
 * A hot observable does not persist any messages (other than during transmit to all
 * subscribers). Thus a subscriber will only receive events from the point where registered
 * with the published.
 */
trait HotObservable[T] extends Observable[T]:
  private var subscribers: Seq[Subscriber[T]] = List[Subscriber[T]]()

  def subscribe(subscriber: Subscriber[T]): Unit = subscribers :+= subscriber

  def publish(event: T): Unit = subscribers.foreach(s => {
      try {
         s.onNext(event)
      } catch {
        case _ => /* Like a boss! */
      }
  })
